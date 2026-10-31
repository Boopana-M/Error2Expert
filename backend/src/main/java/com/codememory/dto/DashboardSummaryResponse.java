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
    private long resolvedMistakes;
    private long openMistakes;
    private int recoveryRate; // Percentage: (resolved / total) * 100
    private String learningVerdict; // e.g. "80% Recovery Rate — Actively learning from errors"
    private String strongestConcept;
    private String weakestConcept;
    private int currentMastery;
}
