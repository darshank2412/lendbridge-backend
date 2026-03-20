package com.lendbridge.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavingsProductRequest {
    @NotBlank @Size(min=3,max=200) private String name;
    @NotNull @DecimalMin("0.00") @DecimalMax("5000000.00") private BigDecimal minBalance;
    @NotNull @DecimalMin("1.00") @DecimalMax("5000000.00") private BigDecimal maxBalance;
    @NotNull @DecimalMin("2.00") @DecimalMax("12.00") private BigDecimal interestRate;
}
