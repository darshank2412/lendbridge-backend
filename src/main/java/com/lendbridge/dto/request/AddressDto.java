package com.lendbridge.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddressDto {
    @NotBlank private String line1;
    @NotBlank private String city;
    @NotBlank private String state;
    @NotBlank @Pattern(regexp = "^[1-9][0-9]{5}$") private String pincode;
}
