package com.lendbridge.dto.request;
import com.lendbridge.enums.DocumentType;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycSubmitRequest {
    @NotNull private DocumentType documentType;
    @NotBlank private String documentNumber;
    private String documentUrl;
}
