package com.lendbridge.dto.request;
import com.lendbridge.enums.RiskAppetite;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LenderPreferenceDto {
    @NotNull private Long loanProductId;
    @NotNull @DecimalMin("8.0") @DecimalMax("24.0") private BigDecimal minInterestRate;
    @NotNull @DecimalMin("8.0") @DecimalMax("24.0") private BigDecimal maxInterestRate;
    @NotNull @Min(6) @Max(60) private Integer minTenureMonths;
    @NotNull @Min(6) @Max(60) private Integer maxTenureMonths;
    @NotNull @DecimalMin("1000.00") @DecimalMax("5000000.00") private BigDecimal minLoanAmount;
    @NotNull @DecimalMin("1000.00") @DecimalMax("5000000.00") private BigDecimal maxLoanAmount;
    @NotNull private RiskAppetite riskAppetite;
}
