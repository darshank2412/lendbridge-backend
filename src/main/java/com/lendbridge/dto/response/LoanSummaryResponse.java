package com.lendbridge.dto.response;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanSummaryResponse {
    private Long loanId;
    private Long loanRequestId;
    private BigDecimal principalAmount;
    private BigDecimal annualInterestRate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal totalPayable;
    private BigDecimal totalInterest;
    private Integer emisPaid;
    private Integer emisRemaining;
    private Integer overdueEmis;
    private LocalDate disbursedDate;
    private LocalDate nextEmiDate;
    private boolean closed;
}
