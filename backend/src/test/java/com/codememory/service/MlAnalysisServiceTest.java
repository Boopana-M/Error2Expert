package com.codememory.service;

import com.codememory.dto.ml.MlAnalysisRequest;
import com.codememory.dto.ml.MlAnalysisResponse;
import com.codememory.entity.enums.MistakeCategory;
import com.codememory.entity.enums.SubmissionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MlAnalysisServiceTest {

    @Autowired
    private MlAnalysisService mlAnalysisService;

    @Test
    @DisplayName("Fallback resilience: when Python ML service is offline, fallback correctly classifies boundary error")
    void testFallbackBoundaryClassification() {
        MlAnalysisRequest request = MlAnalysisRequest.builder()
                .problemTitle("Binary Search")
                .topic(List.of("Binary Search", "Array"))
                .difficulty("EASY")
                .result(SubmissionResult.WRONG_ANSWER)
                .code("int l=0, r=n; while(l<r) { int m=(l+r)/2; high=mid; }")
                .userExplanation("I set high = mid instead of high = mid - 1")
                .build();

        MlAnalysisResponse response = mlAnalysisService.analyzeAttempt(request);

        assertNotNull(response);
        assertEquals(MistakeCategory.BOUNDARY_ERROR, response.getMistakeCategory());
        assertTrue(response.getConfidence() >= 0.85);
        assertNotNull(response.getRootCause());
    }

    @Test
    @DisplayName("Fallback resilience: classifies TLE as COMPLEXITY_ERROR")
    void testFallbackComplexityClassification() {
        MlAnalysisRequest request = MlAnalysisRequest.builder()
                .problemTitle("3Sum")
                .topic(List.of("Array"))
                .result(SubmissionResult.TIME_LIMIT_EXCEEDED)
                .userExplanation("Used 3 nested loops")
                .build();

        MlAnalysisResponse response = mlAnalysisService.analyzeAttempt(request);

        assertNotNull(response);
        assertEquals(MistakeCategory.COMPLEXITY_ERROR, response.getMistakeCategory());
    }
}
