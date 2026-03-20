package com.lendbridge.dto.response;
import com.lendbridge.enums.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanRequestResponse {
    private Long id;
    private Long borrowerId;
    private String borrowerName;
    private Long loanProductId;
    private String loanProductName;
    private BigDecimal amount;
    private Integer tenureMonths;
    private LoanPurpose purpose;
    private String purposeDescription;
    private LoanStatus status;
    private String rejectionReason;
    private Long acceptedByLenderId;
    private String acceptedByLenderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
