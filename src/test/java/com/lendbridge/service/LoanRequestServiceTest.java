package com.lendbridge.service;

import com.lendbridge.dto.request.LoanRequestDto;
import com.lendbridge.entity.*;
import com.lendbridge.enums.*;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.*;
import com.lendbridge.util.EntityMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanRequest Service Unit Tests")
class LoanRequestServiceTest {

    @Mock LoanRequestRepository loanRequestRepo;
    @Mock LenderPreferenceRepository preferenceRepo;
    @Mock UserService userService;
    @Mock LoanProductService loanProductService;
    @Mock EntityMapper mapper;

    @InjectMocks LoanRequestService service;

    private User borrower;
    private User lender;
    private LoanProduct product;
    private LoanRequest pendingRequest;

    @BeforeEach
    void setUp() {
        borrower = User.builder().id(1L).role(Role.BORROWER).fullName("Test Borrower").build();
        lender   = User.builder().id(2L).role(Role.LENDER).fullName("Test Lender").build();
        product  = LoanProduct.builder().id(1L).name("Personal Loan")
                .minAmount(new BigDecimal("10000")).maxAmount(new BigDecimal("500000"))
                .minInterest(new BigDecimal("8")).maxInterest(new BigDecimal("24"))
                .minTenure(6).maxTenure(60).status(ProductStatus.ACTIVE).build();
        pendingRequest = LoanRequest.builder()
                .id(10L).borrower(borrower).loanProduct(product)
                .amount(new BigDecimal("50000")).tenureMonths(12)
                .purpose(LoanPurpose.EDUCATION).status(LoanStatus.PENDING).build();
    }

    // ── Week 4: State transition tests ───────────────────

    @Test
    @DisplayName("Cancel PENDING request succeeds")
    void cancelPendingRequest() {
        when(loanRequestRepo.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(loanRequestRepo.save(any())).thenReturn(pendingRequest);
        when(mapper.toLoanRequestResponse(any())).thenReturn(null);

        service.cancel(10L, 1L);

        assertThat(pendingRequest.getStatus()).isEqualTo(LoanStatus.CANCELLED);
    }

    @Test
    @DisplayName("Cannot cancel MATCHED request")
    void cannotCancelMatchedRequest() {
        pendingRequest.setStatus(LoanStatus.MATCHED);
        when(loanRequestRepo.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> service.cancel(10L, 1L))
                .isInstanceOf(LendbridgeException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("Cannot cancel another borrower's request")
    void cannotCancelOtherBorrowersRequest() {
        when(loanRequestRepo.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> service.cancel(10L, 999L))
                .isInstanceOf(LendbridgeException.class)
                .hasMessageContaining("own");
    }

    @Test
    @DisplayName("Match PENDING → MATCHED succeeds")
    void matchPendingRequest() {
        when(loanRequestRepo.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(loanRequestRepo.save(any())).thenReturn(pendingRequest);
        when(mapper.toLoanRequestResponse(any())).thenReturn(null);

        service.match(10L);

        assertThat(pendingRequest.getStatus()).isEqualTo(LoanStatus.MATCHED);
    }

    @Test
    @DisplayName("Cannot match already MATCHED request")
    void cannotMatchAlreadyMatchedRequest() {
        pendingRequest.setStatus(LoanStatus.MATCHED);
        when(loanRequestRepo.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> service.match(10L))
                .isInstanceOf(LendbridgeException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("Accept MATCHED request assigns lender")
    void acceptMatchedRequest() {
        pendingRequest.setStatus(LoanStatus.MATCHED);
        when(loanRequestRepo.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(userService.findById(2L)).thenReturn(lender);
        when(loanRequestRepo.save(any())).thenReturn(pendingRequest);
        when(mapper.toLoanRequestResponse(any())).thenReturn(null);

        service.accept(10L, 2L);

        assertThat(pendingRequest.getStatus()).isEqualTo(LoanStatus.ACCEPTED);
        assertThat(pendingRequest.getAcceptedByLender()).isEqualTo(lender);
    }

    @Test
    @DisplayName("Cannot accept PENDING (not yet MATCHED) request")
    void cannotAcceptPendingRequest() {
        when(loanRequestRepo.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> service.accept(10L, 2L))
                .isInstanceOf(LendbridgeException.class)
                .hasMessageContaining("MATCHED");
    }

    // ── Week 4: Matchmaking engine ────────────────────────

    @Test
    @DisplayName("Matching request found when within preference ranges")
    void matchingRequestFoundWithinPreference() {
        LenderPreference pref = LenderPreference.builder()
                .lender(lender).loanProduct(product).isActive(true)
                .minLoanAmount(new BigDecimal("10000")).maxLoanAmount(new BigDecimal("200000"))
                .minTenureMonths(6).maxTenureMonths(36)
                .minInterestRate(new BigDecimal("10")).maxInterestRate(new BigDecimal("20"))
                .riskAppetite(RiskAppetite.MEDIUM).build();

        when(preferenceRepo.findActiveByLenderId(2L)).thenReturn(List.of(pref));
        when(loanRequestRepo.findByStatus(LoanStatus.PENDING)).thenReturn(List.of(pendingRequest));
        when(mapper.toLoanRequestResponse(any())).thenReturn(null);

        var results = service.getMatchingRequests(2L);

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Request excluded when amount exceeds preference max")
    void requestExcludedWhenAmountExceedsMax() {
        LenderPreference pref = LenderPreference.builder()
                .lender(lender).loanProduct(product).isActive(true)
                .minLoanAmount(new BigDecimal("1000")).maxLoanAmount(new BigDecimal("40000"))
                .minTenureMonths(6).maxTenureMonths(36)
                .minInterestRate(new BigDecimal("10")).maxInterestRate(new BigDecimal("20"))
                .riskAppetite(RiskAppetite.LOW).build();

        when(preferenceRepo.findActiveByLenderId(2L)).thenReturn(List.of(pref));
        when(loanRequestRepo.findByStatus(LoanStatus.PENDING)).thenReturn(List.of(pendingRequest));

        var results = service.getMatchingRequests(2L);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Empty preferences returns no matching requests")
    void emptyPreferencesReturnsNoMatches() {
        when(preferenceRepo.findActiveByLenderId(2L)).thenReturn(List.of());
        when(loanRequestRepo.findByStatus(LoanStatus.PENDING)).thenReturn(List.of(pendingRequest));

        var results = service.getMatchingRequests(2L);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Reject sets reason and REJECTED status")
    void rejectSetsReasonAndStatus() {
        when(loanRequestRepo.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(loanRequestRepo.save(any())).thenReturn(pendingRequest);
        when(mapper.toLoanRequestResponse(any())).thenReturn(null);

        service.reject(10L, "Insufficient income");

        assertThat(pendingRequest.getStatus()).isEqualTo(LoanStatus.REJECTED);
        assertThat(pendingRequest.getRejectionReason()).isEqualTo("Insufficient income");
    }

    @Test
    @DisplayName("Amount validation rejects out-of-range amounts")
    void amountValidationRejectsOutOfRange() {
        when(userService.findById(1L)).thenReturn(borrower);
        when(loanProductService.findById(1L)).thenReturn(product);

        LoanRequestDto dto = LoanRequestDto.builder()
                .loanProductId(1L).amount(new BigDecimal("5000"))
                .tenureMonths(12).purpose(LoanPurpose.EDUCATION).build();

        assertThatThrownBy(() -> service.create(1L, dto))
                .isInstanceOf(LendbridgeException.class)
                .hasMessageContaining("Amount");
    }
}
