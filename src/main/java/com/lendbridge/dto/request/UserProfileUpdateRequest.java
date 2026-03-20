package com.lendbridge.dto.request;
import com.lendbridge.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileUpdateRequest {
    @Size(max=200) @Pattern(regexp="^[A-Z][a-zA-Z ]*$") private String fullName;
    @Email private String email;
    private Gender gender;
}
