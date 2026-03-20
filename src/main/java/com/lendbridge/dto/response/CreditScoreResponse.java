package com.lendbridge.dto.response;
import lombok.*;
import java.util.List;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditScoreResponse {
    private Long userId;
    private String userName;
    private Integer score;
    private String grade;
    private String recommendation;
    private List<ScoreFactor> factors;
    private String lastUpdated;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ScoreFactor {
        private String label;
        private Integer value;
        private Integer maxValue;
        private String impact;
        private String description;
    }
}
