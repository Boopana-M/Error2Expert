package com.codememory.dto;

import com.codememory.entity.enums.MistakeCategory;
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
public class WeaknessResponse {
    private Long id;
    private String concept;
    private MistakeCategory mistakeCategory;
    private String severity; // HIGH, MEDIUM, IMPROVING
    private Integer frequency;
    private Double confidence;
    private Double masteryScore;
    private LocalDateTime firstObserved;
    private LocalDateTime lastObserved;
    private String description;
    private List<String> affectedProblems;
}
