package com.lendbridge.dto.response;
import com.lendbridge.enums.EmiStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmiScheduleResponse {
    private Long id;
    private Integer emiNumber;
    private LocalDate dueDate;
    private BigDecimal openingBalance;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal emiAmount;
    private BigDecimal closingBalance;
    private EmiStatus status;
    private LocalDate paidDate;
    private BigDecimal amountPaid;
}
