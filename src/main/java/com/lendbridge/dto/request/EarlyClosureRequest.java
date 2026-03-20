package com.lendbridge.dto.request;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EarlyClosureRequest {
    // Amount will be computed server-side; client just confirms
    private String confirmationNote;
}
