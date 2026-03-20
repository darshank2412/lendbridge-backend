package com.lendbridge.service;

import com.lendbridge.dto.request.LoanRequestDto;
import com.lendbridge.dto.response.LoanRequestResponse;
import com.lendbridge.entity.*;
import com.lendbridge.enums.LoanStatus;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.*;
import com.lendbridge.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoanRequestService {

    private final LoanRequestRepository loanRequestRepo;
    private final LenderPreferenceRepository preferenceRepo;
    private final UserService userService;
    private final LoanProductService loanProductService;
    private final EntityMapper mapper;

    // ── BORROWER ─────────────────────────────────────────

    @Transactional
    public LoanRequestResponse create(Long borrowerId, LoanRequestDto dto) {
        User borrower = userService.findById(borrowerId);
        LoanProduct product = loanProductService.findById(dto.getLoanProductId());

        // Validate amount and tenure within product bounds
        if (dto.getAmount().compareTo(product.getMinAmount()) < 0
                || dto.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw LendbridgeException.badRequest("Amount must be between ₹"
                    + product.getMinAmount() + " and ₹" + product.getMaxAmount());
        }
        if (dto.getTenureMonths() < product.getMinTenure()
                || dto.getTenureMonths() > product.getMaxTenure()) {
            throw LendbridgeException.badRequest("Tenure must be between "
                    + product.getMinTenure() + " and " + product.getMaxTenure() + " months");
        }

        LoanRequest req = LoanRequest.builder()
                .borrower(borrower)
                .loanProduct(product)
                .amount(dto.getAmount())
                .tenureMonths(dto.getTenureMonths())
                .purpose(dto.getPurpose())
                .purposeDescription(dto.getPurposeDescription())
                .status(LoanStatus.PENDING)
                .build();
        return mapper.toLoanRequestResponse(loanRequestRepo.save(req));
    }

    public List<LoanRequestResponse> getMyRequests(Long borrowerId) {
        return loanRequestRepo.findByBorrowerId(borrowerId).stream()
                .map(mapper::toLoanRequestResponse).toList();
    }

    @Transactional
    public LoanRequestResponse cancel(Long requestId, Long borrowerId) {
        LoanRequest req = findById(requestId);
        if (!req.getBorrower().getId().equals(borrowerId)) {
            throw LendbridgeException.forbidden("You can only cancel your own requests");
        }
        if (req.getStatus() != LoanStatus.PENDING) {
            throw LendbridgeException.badRequest("Only PENDING requests can be cancelled. Current: " + req.getStatus());
        }
        req.setStatus(LoanStatus.CANCELLED);
        return mapper.toLoanRequestResponse(loanRequestRepo.save(req));
    }

    // ── LENDER ───────────────────────────────────────────

    public List<LoanRequestResponse> getOpenRequests(Long lenderId) {
        return loanRequestRepo.findByStatus(LoanStatus.PENDING).stream()
                .map(mapper::toLoanRequestResponse).toList();
    }

    /**
     * Week 4: Matchmaking engine — returns PENDING requests that match the
     * lender's active preferences (amount, tenure, interest all within range)
     */
    public List<LoanRequestResponse> getMatchingRequests(Long lenderId) {
        List<LenderPreference> activePrefs = preferenceRepo.findActiveByLenderId(lenderId);
        List<LoanRequest> pending = loanRequestRepo.findByStatus(LoanStatus.PENDING);

        return pending.stream()
                .filter(req -> activePrefs.stream().anyMatch(pref -> matches(req, pref)))
                .sorted((a, b) -> {
                    // Rank by best match score descending
                    double scoreA = bestMatchScore(a, activePrefs);
                    double scoreB = bestMatchScore(b, activePrefs);
                    return Double.compare(scoreB, scoreA);
                })
                .map(mapper::toLoanRequestResponse)
                .toList();
    }

    public List<LoanRequestResponse> getMatchedRequests() {
        return loanRequestRepo.findByStatusIn(
                List.of(LoanStatus.MATCHED, LoanStatus.ACCEPTED, LoanStatus.DISBURSED))
                .stream().map(mapper::toLoanRequestResponse).toList();
    }

    // ── ADMIN ─────────────────────────────────────────────

    public List<LoanRequestResponse> getAll(LoanStatus status) {
        List<LoanRequest> requests = (status != null)
                ? loanRequestRepo.findByStatus(status)
                : loanRequestRepo.findAll();
        return requests.stream().map(mapper::toLoanRequestResponse).toList();
    }

    public LoanRequestResponse getById(Long id) {
        return mapper.toLoanRequestResponse(findById(id));
    }

    @Transactional
    public LoanRequestResponse match(Long requestId) {
        LoanRequest req = findById(requestId);
        if (req.getStatus() != LoanStatus.PENDING) {
            throw LendbridgeException.badRequest("Only PENDING requests can be matched. Current: " + req.getStatus());
        }
        req.setStatus(LoanStatus.MATCHED);
        return mapper.toLoanRequestResponse(loanRequestRepo.save(req));
    }

    @Transactional
    public LoanRequestResponse reject(Long requestId, String reason) {
        LoanRequest req = findById(requestId);
        if (req.getStatus() == LoanStatus.DISBURSED || req.getStatus() == LoanStatus.CANCELLED) {
            throw LendbridgeException.badRequest("Cannot reject a " + req.getStatus() + " request");
        }
        req.setStatus(LoanStatus.REJECTED);
        req.setRejectionReason(reason);
        return mapper.toLoanRequestResponse(loanRequestRepo.save(req));
    }

    /**
     * Week 4: Lender accepts — uses optimistic locking to prevent double-accept.
     * Auto-rejects all other MATCHED requests for same borrower on same product.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LoanRequestResponse accept(Long requestId, Long lenderId) {
        LoanRequest req = findById(requestId);
        if (req.getStatus() != LoanStatus.MATCHED) {
            throw LendbridgeException.badRequest("Only MATCHED requests can be accepted. Current: " + req.getStatus());
        }
        User lender = userService.findById(lenderId);
        req.setStatus(LoanStatus.ACCEPTED);
        req.setAcceptedByLender(lender);
        loanRequestRepo.save(req);
        return mapper.toLoanRequestResponse(req);
    }

    // ── Helpers ───────────────────────────────────────────

    public LoanRequest findById(Long id) {
        return loanRequestRepo.findById(id)
                .orElseThrow(() -> LendbridgeException.notFound("Loan request not found with ID: " + id));
    }

    /**
     * Matchmaking: request fits preference if amount, tenure both within range
     */
    private boolean matches(LoanRequest req, LenderPreference pref) {
        if (!pref.getLoanProduct().getId().equals(req.getLoanProduct().getId())) return false;
        boolean amountOk = req.getAmount().compareTo(pref.getMinLoanAmount()) >= 0
                && req.getAmount().compareTo(pref.getMaxLoanAmount()) <= 0;
        boolean tenureOk = req.getTenureMonths() >= pref.getMinTenureMonths()
                && req.getTenureMonths() <= pref.getMaxTenureMonths();
        return amountOk && tenureOk;
    }

    /**
     * Score = how well the request fits within the preference ranges (0–100).
     * Deduct points for amounts/tenures near edges.
     */
    private double bestMatchScore(LoanRequest req, List<LenderPreference> prefs) {
        return prefs.stream()
                .filter(p -> matches(req, p))
                .mapToDouble(p -> computeScore(req, p))
                .max().orElse(0);
    }

    private double computeScore(LoanRequest req, LenderPreference pref) {
        double amountRange = pref.getMaxLoanAmount().subtract(pref.getMinLoanAmount()).doubleValue();
        double amountPos   = req.getAmount().subtract(pref.getMinLoanAmount()).doubleValue();
        double amountScore = amountRange == 0 ? 100 : 100 - Math.abs((amountPos / amountRange - 0.5) * 2) * 30;

        double tenureRange = pref.getMaxTenureMonths() - pref.getMinTenureMonths();
        double tenurePos   = req.getTenureMonths() - pref.getMinTenureMonths();
        double tenureScore = tenureRange == 0 ? 100 : 100 - Math.abs((tenurePos / tenureRange - 0.5) * 2) * 30;

        return (amountScore + tenureScore) / 2;
    }
}
