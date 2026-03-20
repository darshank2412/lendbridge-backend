package com.lendbridge.dto.request;
import com.lendbridge.enums.LoanPurpose;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanRequestDto {
    @NotNull private Long loanProductId;
    @NotNull @DecimalMin("1000.00") @DecimalMax("5000000.00") private BigDecimal amount;
    @NotNull @Min(6) @Max(60) private Integer tenureMonths;
    @NotNull private LoanPurpose purpose;
    @Size(max=500) private String purposeDescription;
}
