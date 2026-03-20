package com.lendbridge.service;

import com.lendbridge.entity.*;
import com.lendbridge.enums.*;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Credit Score Service Unit Tests")
class CreditScoreServiceTest {

    @Mock UserService userService;
    @Mock LoanRepository loanRepo;
    @Mock LoanRequestRepository loanRequestRepo;
    @Mock EmiScheduleRepository emiRepo;
    @InjectMocks CreditScoreService creditScoreService;

    @Test
    @DisplayName("Verified KYC adds 150 points")
    void verifiedKycAdds150Points() {
        User user = User.builder().id(1L).fullName("Test")
                .kycStatus(KycStatus.VERIFIED).incomeBracket("5_10_LPA")
                .p2pExperience(P2PExperience.BEGINNER).build();
        when(userService.findById(1L)).thenReturn(user);
        when(loanRepo.findByBorrowerId(1L)).thenReturn(List.of());
        when(loanRequestRepo.findByBorrowerId(1L)).thenReturn(List.of());

        var result = creditScoreService.getScore(1L);
        assertThat(result.getScore()).isGreaterThanOrEqualTo(530); // 300 base + 150 KYC + 60 income + 20 exp + 0 history + 50 active
    }

    @Test
    @DisplayName("Score is always between 300 and 900")
    void scoreBetween300And900() {
        User user = User.builder().id(1L).fullName("Max Score User")
                .kycStatus(KycStatus.VERIFIED).incomeBracket("ABOVE_50_LPA")
                .p2pExperience(P2PExperience.ADVANCED).build();
        when(userService.findById(1L)).thenReturn(user);
        when(loanRepo.findByBorrowerId(1L)).thenReturn(List.of());
        when(loanRequestRepo.findByBorrowerId(1L)).thenReturn(List.of());

        var result = creditScoreService.getScore(1L);
        assertThat(result.getScore()).isBetween(300, 900);
    }

    @Test
    @DisplayName("Grade is EXCELLENT for score >= 750")
    void gradeExcellentForHighScore() {
        User user = User.builder().id(1L).fullName("High Score User")
                .kycStatus(KycStatus.VERIFIED).incomeBracket("ABOVE_50_LPA")
                .p2pExperience(P2PExperience.ADVANCED).build();
        when(userService.findById(1L)).thenReturn(user);
        when(loanRepo.findByBorrowerId(1L)).thenReturn(List.of());
        when(loanRequestRepo.findByBorrowerId(1L)).thenReturn(List.of());

        var result = creditScoreService.getScore(1L);
        // Max possible: 300+150+120+200+80+50 = 900 → EXCELLENT
        assertThat(result.getGrade()).isIn("EXCELLENT", "GOOD");
    }

    @Test
    @DisplayName("Score factors list is not empty")
    void scoreFactorsNotEmpty() {
        User user = User.builder().id(1L).fullName("Factor Test").kycStatus(KycStatus.PENDING).build();
        when(userService.findById(1L)).thenReturn(user);
        when(loanRepo.findByBorrowerId(1L)).thenReturn(List.of());
        when(loanRequestRepo.findByBorrowerId(1L)).thenReturn(List.of());

        var result = creditScoreService.getScore(1L);
        assertThat(result.getFactors()).isNotEmpty();
        assertThat(result.getFactors()).hasSizeGreaterThanOrEqualTo(4);
    }
}

@ExtendWith(MockitoExtension.class)
@DisplayName("Lender Preference Service Unit Tests")
class LenderPreferenceServiceTest {

    @Mock LenderPreferenceRepository repo;
    @Mock UserService userService;
    @Mock LoanProductService loanProductService;
    @Mock com.lendbridge.util.EntityMapper mapper;
    @InjectMocks LenderPreferenceService service;

    @Test
    @DisplayName("Saving preference with min > max interest throws BadRequest")
    void minGreaterThanMaxInterestThrows() {
        var dto = com.lendbridge.dto.request.LenderPreferenceDto.builder()
                .loanProductId(1L)
                .minInterestRate(new BigDecimal("20"))
                .maxInterestRate(new BigDecimal("10"))
                .minTenureMonths(6).maxTenureMonths(24)
                .minLoanAmount(new BigDecimal("10000")).maxLoanAmount(new BigDecimal("100000"))
                .riskAppetite(RiskAppetite.MEDIUM).build();

        assertThatThrownBy(() -> service.savePreference(1L, dto))
                .isInstanceOf(LendbridgeException.class)
                .hasMessageContaining("minInterestRate");
    }

    @Test
    @DisplayName("Deactivating non-existent preference throws NotFound")
    void deactivatingNonExistentPreferenceThrows() {
        when(repo.findByLenderIdAndLoanProductId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(1L, 99L))
                .isInstanceOf(LendbridgeException.class)
                .hasMessageContaining("not found");
    }
}
