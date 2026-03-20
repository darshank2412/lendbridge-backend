package com.lendbridge.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartialRepaymentRequest {
    @NotNull @DecimalMin("1.00") private BigDecimal amount;
}
