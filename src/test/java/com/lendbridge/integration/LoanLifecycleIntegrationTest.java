package com.lendbridge.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lendbridge.dto.request.*;
import com.lendbridge.entity.*;
import com.lendbridge.enums.*;
import com.lendbridge.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Loan Lifecycle Integration Tests")
class LoanLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepo;
    @Autowired LoanProductRepository loanProductRepo;
    @Autowired LoanRequestRepository loanRequestRepo;
    @Autowired LoanRepository loanRepo;
    @Autowired ObjectMapper objectMapper;

    @LocalServerPort int port;

    static Long borrowerToken_holder;
    static Long lenderToken_holder;
    static Long adminToken_holder;
    static Long loanProductId;
    static Long loanRequestId;
    static Long loanId;
    static String borrowerToken;
    static String lenderToken;
    static String adminToken;

    private String base() { return "http://localhost:" + port; }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        return h;
    }

    @Test @Order(1)
    @DisplayName("Setup: create admin, loan product, borrower, lender")
    void setup() {
        // Create admin directly in DB
        User admin = userRepo.save(User.builder()
                .phoneNumber("9999999999").countryCode("+91")
                .fullName("Test Admin").email("admin@test.com")
                .password("$2a$10$bZzDcupqEhNp7Q9HQAO3LOUAn3xdlw/yS2u3FxFz7gBHHAMlz/mJ2") // Admin@123
                .role(Role.ADMIN).status(UserStatus.REGISTRATION_COMPLETE)
                .platformAccountNumber("LBADMIN001").build());

        // Login admin
        var loginBody = Map.of("phoneNumber", "9999999999", "password", "Admin@123");
        var loginResp = restTemplate.postForEntity(base() + "/auth/login",
                new HttpEntity<>(loginBody, bearer(null)), Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = (String) ((Map) loginResp.getBody().get("data")).get("token");

        // Create loan product
        var prodReq = LoanProductRequest.builder()
                .name("Test Personal Loan")
                .minAmount(new BigDecimal("10000")).maxAmount(new BigDecimal("500000"))
                .minInterest(new BigDecimal("10")).maxInterest(new BigDecimal("20"))
                .minTenure(6).maxTenure(60).build();
        var prodResp = restTemplate.exchange(base() + "/loan-products",
                HttpMethod.POST,
                new HttpEntity<>(prodReq, bearer(adminToken)), Map.class);
        assertThat(prodResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        loanProductId = Long.valueOf(
                ((Map)((Map)prodResp.getBody().get("data"))).get("id").toString());
    }

    @Test @Order(2)
    @DisplayName("Borrower registration and login")
    void borrowerRegistration() {
        // Simulate OTP flow by creating user directly
        User borrower = userRepo.save(User.builder()
                .phoneNumber("9876543210").countryCode("+91")
                .role(Role.BORROWER).status(UserStatus.MOBILE_VERIFIED)
                .password("$2a$10$bZzDcupqEhNp7Q9HQAO3LOUAn3xdlw/yS2u3FxFz7gBHHAMlz/mJ2") // Admin@123
                .build());

        var regReq = UserRegistrationRequest.builder()
                .firstName("Arjun").lastName("Sharma")
                .email("arjun@test.com").phoneNumber("9876543210")
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .gender(Gender.MALE).role(Role.BORROWER)
                .pan("ABCDE1234F").incomeBracket("5_10_LPA")
                .p2pExperience(P2PExperience.BEGINNER)
                .address(new AddressDto("123 MG Road", "Mumbai", "Maharashtra", "400001"))
                .password("Admin@123").build();

        var regResp = restTemplate.exchange(
                base() + "/register?userId=" + borrower.getId(),
                HttpMethod.POST,
                new HttpEntity<>(regReq, bearer(null)), Map.class);
        assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        var login = restTemplate.postForEntity(base() + "/auth/login",
                new HttpEntity<>(Map.of("phoneNumber","9876543210","password","Admin@123"), bearer(null)), Map.class);
        borrowerToken = (String) ((Map) login.getBody().get("data")).get("token");
        assertThat(borrowerToken).isNotBlank();
    }

    @Test @Order(3)
    @DisplayName("Borrower submits loan request")
    void borrowerSubmitsRequest() {
        User borrower = userRepo.findByPhoneNumber("9876543210").orElseThrow();
        var dto = LoanRequestDto.builder()
                .loanProductId(loanProductId)
                .amount(new BigDecimal("50000"))
                .tenureMonths(12)
                .purpose(LoanPurpose.EDUCATION)
                .purposeDescription("College tuition fees")
                .build();

        var resp = restTemplate.exchange(
                base() + "/loan-requests?borrowerId=" + borrower.getId(),
                HttpMethod.POST,
                new HttpEntity<>(dto, bearer(borrowerToken)), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map)resp.getBody().get("data")).get("status")).isEqualTo("PENDING");
        loanRequestId = Long.valueOf(((Map)resp.getBody().get("data")).get("id").toString());
    }

    @Test @Order(4)
    @DisplayName("Admin matches the loan request")
    void adminMatchesRequest() {
        var resp = restTemplate.exchange(
                base() + "/loan-requests/" + loanRequestId + "/match",
                HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map)resp.getBody().get("data")).get("status")).isEqualTo("MATCHED");
    }

    @Test @Order(5)
    @DisplayName("Lender registers and accepts the matched request")
    void lenderAcceptsRequest() {
        // Create lender
        User lender = userRepo.save(User.builder()
                .phoneNumber("9876543211").countryCode("+91")
                .role(Role.LENDER).status(UserStatus.REGISTRATION_COMPLETE)
                .fullName("Test Lender").email("lender@test.com")
                .password("$2a$10$bZzDcupqEhNp7Q9HQAO3LOUAn3xdlw/yS2u3FxFz7gBHHAMlz/mJ2")
                .platformAccountNumber("LBLENDER001").build());

        var login = restTemplate.postForEntity(base() + "/auth/login",
                new HttpEntity<>(Map.of("phoneNumber","9876543211","password","Admin@123"), bearer(null)), Map.class);
        lenderToken = (String) ((Map) login.getBody().get("data")).get("token");

        var resp = restTemplate.exchange(
                base() + "/loan-requests/" + loanRequestId + "/accept?lenderId=" + lender.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(bearer(lenderToken)), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map)resp.getBody().get("data")).get("status")).isEqualTo("ACCEPTED");
    }

    @Test @Order(6)
    @DisplayName("Admin disburses the accepted loan")
    void adminDisbursesLoan() {
        var resp = restTemplate.exchange(
                base() + "/disbursements/" + loanRequestId,
                HttpMethod.POST,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map)resp.getBody().get("data")).get("status")).isEqualTo("DISBURSED");

        // Loan should now exist
        var loan = loanRepo.findByLoanRequestId(loanRequestId);
        assertThat(loan).isPresent();
        loanId = loan.get().getId();
    }

    @Test @Order(7)
    @DisplayName("EMI schedule generated with correct number of rows")
    void emiScheduleGenerated() {
        var resp = restTemplate.exchange(
                base() + "/emi-schedule/" + loanId,
                HttpMethod.GET,
                new HttpEntity<>(bearer(borrowerToken)), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var data = (java.util.List) resp.getBody().get("data");
        assertThat(data).hasSize(12); // 12 months tenure
    }

    @Test @Order(8)
    @DisplayName("Cannot disburse same loan twice")
    void cannotDisburseTwice() {
        var resp = restTemplate.exchange(
                base() + "/disbursements/" + loanRequestId,
                HttpMethod.POST,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test @Order(9)
    @DisplayName("Credit score endpoint returns valid score")
    void creditScoreReturnsValidScore() {
        User borrower = userRepo.findByPhoneNumber("9876543210").orElseThrow();
        var resp = restTemplate.exchange(
                base() + "/credit-score/" + borrower.getId(),
                HttpMethod.GET,
                new HttpEntity<>(bearer(borrowerToken)), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var data = (Map) resp.getBody().get("data");
        int score = (int) data.get("score");
        assertThat(score).isBetween(300, 900);
    }

    @Test @Order(10)
    @DisplayName("Loan summary shows correct EMI count")
    void loanSummaryCorrect() {
        var resp = restTemplate.exchange(
                base() + "/loans/" + loanId + "/summary",
                HttpMethod.GET,
                new HttpEntity<>(bearer(borrowerToken)), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var data = (Map) resp.getBody().get("data");
        assertThat(data.get("emisPaid")).isEqualTo(0);
        assertThat(data.get("emisRemaining")).isEqualTo(12);
    }
}
