package com.lendbridge.service;

import com.lendbridge.dto.request.LenderPreferenceDto;
import com.lendbridge.dto.response.LenderPreferenceResponse;
import com.lendbridge.entity.*;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.LenderPreferenceRepository;
import com.lendbridge.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LenderPreferenceService {

    private final LenderPreferenceRepository repo;
    private final UserService userService;
    private final LoanProductService loanProductService;
    private final EntityMapper mapper;

    public List<LenderPreferenceResponse> getAllActive() {
        return repo.findByIsActiveTrue().stream()
                .map(mapper::toLenderPreferenceResponse).toList();
    }

    public List<LenderPreferenceResponse> getMyPreferences(Long lenderId) {
        return repo.findByLenderId(lenderId).stream()
                .map(mapper::toLenderPreferenceResponse).toList();
    }

    @Transactional
    public LenderPreferenceResponse savePreference(Long lenderId, LenderPreferenceDto dto) {
        validate(dto);
        User lender = userService.findById(lenderId);
        LoanProduct product = loanProductService.findById(dto.getLoanProductId());

        Optional<LenderPreference> existing =
                repo.findByLenderIdAndLoanProductId(lenderId, dto.getLoanProductId());

        LenderPreference pref = existing.orElse(LenderPreference.builder()
                .lender(lender).loanProduct(product).build());

        pref.setMinInterestRate(dto.getMinInterestRate());
        pref.setMaxInterestRate(dto.getMaxInterestRate());
        pref.setMinTenureMonths(dto.getMinTenureMonths());
        pref.setMaxTenureMonths(dto.getMaxTenureMonths());
        pref.setMinLoanAmount(dto.getMinLoanAmount());
        pref.setMaxLoanAmount(dto.getMaxLoanAmount());
        pref.setRiskAppetite(dto.getRiskAppetite());
        pref.setIsActive(true);

        return mapper.toLenderPreferenceResponse(repo.save(pref));
    }

    @Transactional
    public LenderPreferenceResponse deactivate(Long lenderId, Long loanProductId) {
        LenderPreference pref = repo.findByLenderIdAndLoanProductId(lenderId, loanProductId)
                .orElseThrow(() -> LendbridgeException.notFound("Preference not found"));
        pref.setIsActive(false);
        return mapper.toLenderPreferenceResponse(repo.save(pref));
    }

    private void validate(LenderPreferenceDto dto) {
        if (dto.getMinInterestRate().compareTo(dto.getMaxInterestRate()) >= 0)
            throw LendbridgeException.badRequest("minInterestRate must be less than maxInterestRate");
        if (dto.getMinTenureMonths() >= dto.getMaxTenureMonths())
            throw LendbridgeException.badRequest("minTenureMonths must be less than maxTenureMonths");
        if (dto.getMinLoanAmount().compareTo(dto.getMaxLoanAmount()) >= 0)
            throw LendbridgeException.badRequest("minLoanAmount must be less than maxLoanAmount");
    }
}
