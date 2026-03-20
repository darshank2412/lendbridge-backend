package com.lendbridge.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateAdminRequest {
    @NotBlank @Pattern(regexp="^[6-9][0-9]{9}$") private String phoneNumber;
    @NotBlank @Pattern(regexp="^\\+\\d{1,3}$") private String countryCode;
    @NotBlank @Pattern(regexp="^[A-Z][a-zA-Z ]*$") @Size(max=200) private String fullName;
    @NotBlank @Email private String email;
    @NotBlank @Pattern(regexp="^(?=.*[A-Z])(?=.*[a-z])(?=.*[@$!%*?&]).{8,}$") private String password;
}
