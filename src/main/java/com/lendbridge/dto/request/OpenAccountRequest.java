package com.lendbridge.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OpenAccountRequest {
    @NotNull private Long userId;
    @NotNull private Long productId;
}
