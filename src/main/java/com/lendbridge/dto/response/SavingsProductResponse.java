package com.lendbridge.dto.response;
import com.lendbridge.enums.ProductStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavingsProductResponse {
    private Long id;
    private String name;
    private BigDecimal minBalance;
    private BigDecimal maxBalance;
    private BigDecimal interestRate;
    private ProductStatus status;
    private LocalDateTime createdAt;
}
