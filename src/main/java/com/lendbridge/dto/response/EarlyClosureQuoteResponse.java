package com.lendbridge.dto.response;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EarlyClosureQuoteResponse {
    private BigDecimal outstandingPrincipal;
    private BigDecimal penaltyRate;
    private BigDecimal penaltyAmount;
    private BigDecimal totalPayable;
    private String note;
}
