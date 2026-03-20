package com.lendbridge.dto.request;
import com.lendbridge.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpVerifyRequest {
    @NotBlank @Pattern(regexp = "^([6-9][0-9]{9}|[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})$")
    private String identifier;
    @NotBlank @Pattern(regexp = "^[0-9]{6}$")
    private String otpCode;
    @NotNull private OtpType otpType;
    @NotNull private OtpPurpose purpose;
    @NotBlank @Pattern(regexp = "^\\+[1-9][0-9]{0,3}$")
    private String countryCode;
    @NotNull private Role role;
}
