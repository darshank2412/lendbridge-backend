package com.lendbridge.service;

import com.lendbridge.dto.request.PartialRepaymentRequest;
import com.lendbridge.dto.response.*;
import com.lendbridge.entity.*;
import com.lendbridge.enums.EmiStatus;
import com.lendbridge.enums.LoanStatus;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.*;
import com.lendbridge.util.EmiCalculator;
import com.lendbridge.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private static final BigDecimal EARLY_CLOSURE_PENALTY_RATE = new BigDecimal("0.02"); // 2%
    private static final BigDecimal ANNUAL_RATE_DEFAULT = new BigDecimal("12"); // fallback

    private final LoanRepository loanRepo;
    private final EmiScheduleRepository emiRepo;
    private final LoanRequestRepository loanRequestRepo;
    private final BankAccountRepository bankAccountRepo;
    private final EntityMapper mapper;

    // ── Disbursement (Admin — Week 5) ─────────────────────

    @Transactional
    public LoanRequestResponse disburse(Long requestId) {
        LoanRequest request = loanRequestRepo.findById(requestId)
                .orElseThrow(() -> LendbridgeException.notFound("Loan request not found: " + requestId));

        if (request.getStatus() != LoanStatus.ACCEPTED) {
            throw LendbridgeException.badRequest(
                    "Only ACCEPTED requests can be disbursed. Current: " + request.getStatus());
        }
        if (loanRepo.findByLoanRequestId(requestId).isPresent()) {
            throw LendbridgeException.conflict("Loan already disbursed for this request");
        }

        // Use midpoint of product's interest range as disbursed rate
        BigDecimal annualRate = request.getLoanProduct().getMinInterest()
                .add(request.getLoanProduct().getMaxInterest())
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        BigDecimal emi = EmiCalculator.calculateEmi(
                request.getAmount(), annualRate, request.getTenureMonths());

        LocalDate today = LocalDate.now();
        Loan loan = Loan.builder()
                .loanRequest(request)
                .borrower(request.getBorrower())
                .lender(request.getAcceptedByLender())
                .principalAmount(request.getAmount())
                .annualInterestRate(annualRate)
                .tenureMonths(request.getTenureMonths())
                .emiAmount(emi)
                .outstandingBalance(request.getAmount())
                .disbursedDate(today)
                .nextEmiDate(today.plusMonths(1))
                .build();

        loan = loanRepo.save(loan);
        generateEmiSchedule(loan);

        request.setStatus(LoanStatus.DISBURSED);
        loanRequestRepo.save(request);

        log.info("Loan disbursed: requestId={}, amount={}, emi={}, rate={}%",
                requestId, request.getAmount(), emi, annualRate);

        return buildLoanRequestResponse(request);
    }

    // ── EMI Schedule ──────────────────────────────────────

    public List<EmiScheduleResponse> getEmiSchedule(Long loanId) {
        return emiRepo.findByLoanIdOrderByEmiNumber(loanId).stream()
                .map(mapper::toEmiScheduleResponse).toList();
    }

    // ── Loan Summary ──────────────────────────────────────

    public LoanSummaryResponse getLoanSummary(Long loanId) {
        Loan loan = findLoanById(loanId);
        List<EmiSchedule> schedules = emiRepo.findByLoanIdOrderByEmiNumber(loanId);
        long paid    = schedules.stream().filter(e -> e.getStatus() == EmiStatus.PAID).count();
        long overdue = schedules.stream().filter(e -> e.getStatus() == EmiStatus.OVERDUE).count();
        long remaining = schedules.stream().filter(e -> e.getStatus() == EmiStatus.PENDING
                || e.getStatus() == EmiStatus.OVERDUE).count();

        BigDecimal totalPayable = loan.getEmiAmount()
                .multiply(BigDecimal.valueOf(loan.getTenureMonths()));
        BigDecimal totalInterest = totalPayable.subtract(loan.getPrincipalAmount());

        return LoanSummaryResponse.builder()
                .loanId(loan.getId())
                .loanRequestId(loan.getLoanRequest().getId())
                .principalAmount(loan.getPrincipalAmount())
                .annualInterestRate(loan.getAnnualInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .emiAmount(loan.getEmiAmount())
                .outstandingBalance(loan.getOutstandingBalance())
                .totalPayable(totalPayable)
                .totalInterest(totalInterest)
                .emisPaid((int) paid)
                .emisRemaining((int) remaining)
                .overdueEmis((int) overdue)
                .disbursedDate(loan.getDisbursedDate())
                .nextEmiDate(loan.getNextEmiDate())
                .closed(loan.isClosed())
                .build();
    }

    // ── Pay EMI (Week 5) ──────────────────────────────────

    @Transactional
    public EmiScheduleResponse payNextEmi(Long loanId) {
        Loan loan = findLoanById(loanId);
        if (loan.isClosed()) throw LendbridgeException.badRequest("Loan is already closed.");

        EmiSchedule nextEmi = emiRepo.findByLoanIdOrderByEmiNumber(loanId).stream()
                .filter(e -> e.getStatus() != EmiStatus.PAID)
                .findFirst()
                .orElseThrow(() -> LendbridgeException.badRequest("No pending EMIs found."));

        // Debit borrower savings account
        bankAccountRepo.findByUserIdAndAccountType(
                loan.getBorrower().getId(),
                com.lendbridge.enums.AccountType.SAVINGS)
                .ifPresent(acc -> {
                    if (acc.getBalance().compareTo(nextEmi.getEmiAmount()) < 0) {
                        throw LendbridgeException.badRequest("Insufficient balance in savings account.");
                    }
                    acc.setBalance(acc.getBalance().subtract(nextEmi.getEmiAmount()));
                    bankAccountRepo.save(acc);
                });

        // Credit lender savings account
        bankAccountRepo.findByUserIdAndAccountType(
                loan.getLender().getId(),
                com.lendbridge.enums.AccountType.SAVINGS)
                .ifPresent(acc -> {
                    acc.setBalance(acc.getBalance().add(nextEmi.getEmiAmount()));
                    bankAccountRepo.save(acc);
                });

        nextEmi.setStatus(EmiStatus.PAID);
        nextEmi.setPaidDate(LocalDate.now());
        nextEmi.setAmountPaid(nextEmi.getEmiAmount());
        emiRepo.save(nextEmi);

        loan.setEmisPaid(loan.getEmisPaid() + 1);
        loan.setOutstandingBalance(nextEmi.getClosingBalance());
        loan.setNextEmiDate(loan.getNextEmiDate().plusMonths(1));

        if (loan.getEmisPaid().equals(loan.getTenureMonths())) {
            loan.setClosed(true);
            loan.setClosedDate(LocalDate.now());
        }
        loanRepo.save(loan);

        return mapper.toEmiScheduleResponse(nextEmi);
    }

    // ── Partial Repayment (Week 7) ────────────────────────

    @Transactional
    public LoanSummaryResponse partialRepayment(Long loanId, PartialRepaymentRequest req) {
        Loan loan = findLoanById(loanId);
        if (loan.isClosed()) throw LendbridgeException.badRequest("Loan is already closed.");
        if (req.getAmount().compareTo(loan.getOutstandingBalance()) > 0) {
            throw LendbridgeException.badRequest("Partial amount exceeds outstanding balance.");
        }

        // Reduce outstanding balance
        BigDecimal newBalance = loan.getOutstandingBalance().subtract(req.getAmount());
        loan.setOutstandingBalance(newBalance);

        // Recalculate remaining EMIs
        int remainingEmis = (int) emiRepo.findByLoanIdOrderByEmiNumber(loanId).stream()
                .filter(e -> e.getStatus() != EmiStatus.PAID).count();

        if (remainingEmis > 0 && newBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newEmi = EmiCalculator.calculateEmi(
                    newBalance, loan.getAnnualInterestRate(), remainingEmis);
            loan.setEmiAmount(newEmi);
            // Update pending EMI schedule entries
            regeneratePendingSchedule(loan, newBalance, remainingEmis);
        } else {
            loan.setClosed(true);
            loan.setClosedDate(LocalDate.now());
        }
        loanRepo.save(loan);
        return getLoanSummary(loanId);
    }

    // ── Early Closure Quote (Week 7) ──────────────────────

    public EarlyClosureQuoteResponse getEarlyClosureQuote(Long loanId) {
        Loan loan = findLoanById(loanId);
        BigDecimal outstanding = loan.getOutstandingBalance();
        BigDecimal penalty = outstanding.multiply(EARLY_CLOSURE_PENALTY_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        return EarlyClosureQuoteResponse.builder()
                .outstandingPrincipal(outstanding)
                .penaltyRate(new BigDecimal("2.00"))
                .penaltyAmount(penalty)
                .totalPayable(outstanding.add(penalty))
                .note("2% early closure penalty applies on outstanding principal")
                .build();
    }

    // ── Early Closure (Week 7) ────────────────────────────

    @Transactional
    public LoanSummaryResponse earlyClose(Long loanId) {
        Loan loan = findLoanById(loanId);
        if (loan.isClosed()) throw LendbridgeException.badRequest("Loan already closed.");

        EarlyClosureQuoteResponse quote = getEarlyClosureQuote(loanId);
        BigDecimal totalDue = quote.getTotalPayable();

        // Debit borrower
        bankAccountRepo.findByUserIdAndAccountType(
                loan.getBorrower().getId(),
                com.lendbridge.enums.AccountType.SAVINGS)
                .ifPresent(acc -> {
                    if (acc.getBalance().compareTo(totalDue) < 0)
                        throw LendbridgeException.badRequest("Insufficient balance for early closure.");
                    acc.setBalance(acc.getBalance().subtract(totalDue));
                    bankAccountRepo.save(acc);
                });

        // Mark all remaining EMIs as paid
        emiRepo.findByLoanIdOrderByEmiNumber(loanId).stream()
                .filter(e -> e.getStatus() != EmiStatus.PAID)
                .forEach(e -> {
                    e.setStatus(EmiStatus.PAID);
                    e.setPaidDate(LocalDate.now());
                    emiRepo.save(e);
                });

        loan.setOutstandingBalance(BigDecimal.ZERO);
        loan.setClosed(true);
        loan.setClosedDate(LocalDate.now());
        loanRepo.save(loan);

        return getLoanSummary(loanId);
    }

    // ── Active loans for borrower ─────────────────────────

    public List<LoanSummaryResponse> getActiveLoansByBorrower(Long borrowerId) {
        return loanRepo.findByBorrowerIdAndClosedFalse(borrowerId).stream()
                .map(l -> getLoanSummary(l.getId())).toList();
    }

    // ── Helpers ───────────────────────────────────────────

    private void generateEmiSchedule(Loan loan) {
        BigDecimal r = loan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal balance = loan.getPrincipalAmount();
        List<EmiSchedule> schedules = new ArrayList<>();

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            BigDecimal interest = balance.multiply(r).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principal = loan.getEmiAmount().subtract(interest).setScale(2, RoundingMode.HALF_UP);
            BigDecimal closing = balance.subtract(principal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            schedules.add(EmiSchedule.builder()
                    .loan(loan)
                    .emiNumber(i)
                    .dueDate(loan.getDisbursedDate().plusMonths(i))
                    .openingBalance(balance.setScale(2, RoundingMode.HALF_UP))
                    .principalComponent(principal)
                    .interestComponent(interest)
                    .emiAmount(loan.getEmiAmount())
                    .closingBalance(closing)
                    .status(EmiStatus.PENDING)
                    .build());
            balance = closing;
        }
        emiRepo.saveAll(schedules);
    }

    private void regeneratePendingSchedule(Loan loan, BigDecimal balance, int remaining) {
        List<EmiSchedule> pending = emiRepo.findByLoanIdOrderByEmiNumber(loan.getId()).stream()
                .filter(e -> e.getStatus() != EmiStatus.PAID).toList();

        BigDecimal r = loan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        int idx = 0;
        for (EmiSchedule emi : pending) {
            BigDecimal interest = balance.multiply(r).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principal = loan.getEmiAmount().subtract(interest).setScale(2, RoundingMode.HALF_UP);
            BigDecimal closing = balance.subtract(principal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            emi.setOpeningBalance(balance.setScale(2, RoundingMode.HALF_UP));
            emi.setPrincipalComponent(principal);
            emi.setInterestComponent(interest);
            emi.setEmiAmount(loan.getEmiAmount());
            emi.setClosingBalance(closing);
            balance = closing;
        }
        emiRepo.saveAll(pending);
    }

    private LoanRequestResponse buildLoanRequestResponse(LoanRequest r) {
        return mapper.toLoanRequestResponse(r);
    }

    public Loan findLoanById(Long id) {
        return loanRepo.findById(id)
                .orElseThrow(() -> LendbridgeException.notFound("Loan not found: " + id));
    }
}
