package com.lendbridge.dto.request;
import com.lendbridge.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRegistrationRequest {
    @NotBlank @Size(min=2,max=100) @Pattern(regexp="^[a-zA-Z\\s'-]+$") private String firstName;
    @NotBlank @Size(min=2,max=100) @Pattern(regexp="^[a-zA-Z\\s'-]+$") private String lastName;
    @NotBlank @Email private String email;
    @NotBlank @Pattern(regexp="^[0-9]{10}$") private String phoneNumber;
    @NotNull private LocalDate dateOfBirth;
    @NotNull private Gender gender;
    @NotNull private Role role;
    @NotBlank @Pattern(regexp="[A-Z]{5}[0-9]{4}[A-Z]{1}") private String pan;
    @NotBlank private String incomeBracket;
    private P2PExperience p2pExperience;
    @NotNull private AddressDto address;
    @NotBlank @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?`~])[^\\s]{8,}$")
    private String password;
}
