package com.codememory.dto;

import com.codememory.entity.enums.RecommendationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private Long id;
    private Long weaknessId;
    private String weaknessConcept;
    private Long problemId;
    private String leetcodeId;
    private String title;
    private String difficulty;
    private List<String> topics;
    private String url;
    private String reason;
    private RecommendationStatus status;
    private LocalDateTime createdAt;
}
