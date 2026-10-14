package com.codememory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodaysFocusResponse {
    private String weakness;
    private String why;
    private List<RecommendationResponse> recommendedProblems;
}
