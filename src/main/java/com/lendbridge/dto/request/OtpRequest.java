package com.lendbridge.dto.request;

import com.lendbridge.enums.OtpPurpose;
import com.lendbridge.enums.OtpType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpRequest {

    @NotBlank
    @Pattern(regexp = "^([6-9][0-9]{9}|[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})$")
    private String identifier;

    @NotNull
    private OtpType otpType;

    @NotNull
    private OtpPurpose purpose;

    @Pattern(regexp = "^\\+[1-9][0-9]{0,3}$")
    private String countryCode;
}
