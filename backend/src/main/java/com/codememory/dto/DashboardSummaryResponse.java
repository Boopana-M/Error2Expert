package com.codememory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {
    private long problemsAttempted;
    private long problemsAccepted;
    private long totalMistakes;
    private long recurringMistakes;
    private String strongestConcept;
    private String weakestConcept;
    private int currentMastery;
}
