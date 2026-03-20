package com.lendbridge.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanProductRequest {
    @NotBlank @Size(min=3,max=200) private String name;
    @NotNull @DecimalMin("1000.00") @DecimalMax("5000000.00") private BigDecimal minAmount;
    @NotNull @DecimalMin("1000.00") @DecimalMax("5000000.00") private BigDecimal maxAmount;
    @NotNull @DecimalMin("8.00") @DecimalMax("24.00") private BigDecimal minInterest;
    @NotNull @DecimalMin("8.00") @DecimalMax("24.00") private BigDecimal maxInterest;
    @NotNull @Min(6) @Max(60) private Integer minTenure;
    @NotNull @Min(6) @Max(60) private Integer maxTenure;
}
