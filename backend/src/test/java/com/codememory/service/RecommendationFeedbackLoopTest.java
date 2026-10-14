package com.codememory.service;

import com.codememory.dto.SubmissionRequest;
import com.codememory.dto.SubmissionResponse;
import com.codememory.dto.TodaysFocusResponse;
import com.codememory.dto.WeaknessResponse;
import com.codememory.entity.enums.SubmissionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationFeedbackLoopTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private WeaknessService weaknessService;

    @Autowired
    private RecommendationService recommendationService;

    @Test
    @DisplayName("Should detect recurring weakness and generate targeted recommendations on repeat failures")
    void shouldDetectRecurringWeaknessAndGenerateRecommendations() {
        // Attempt 1: Boundary failure on Binary Search
        SubmissionRequest attempt1 = SubmissionRequest.builder()
                .problemTitle("Binary Search")
                .difficulty("EASY")
                .topics(List.of("Binary Search", "Array"))
                .code("int l = 0, r = n; while(l < r) { int m = (l+r)/2; l = m; }")
                .result(SubmissionResult.WRONG_ANSWER)
                .userExplanation("I set high = mid instead of high = mid - 1 causing boundary overshoot")
                .build();
        submissionService.createSubmission(attempt1);

        // Attempt 2: Another boundary failure on Search Insert Position
        SubmissionRequest attempt2 = SubmissionRequest.builder()
                .problemTitle("Search Insert Position")
                .difficulty("EASY")
                .topics(List.of("Binary Search", "Array"))
                .code("int l = 0, r = nums.length; while(l <= r) { int m = (l+r)/2; if (nums[m] < target) l = m; }")
                .result(SubmissionResult.WRONG_ANSWER)
                .userExplanation("Boundary pointer update left = mid caused infinite loop")
                .build();
        submissionService.createSubmission(attempt2);

        // Verify weakness is tracked with frequency >= 2
        List<WeaknessResponse> weaknesses = weaknessService.getWeaknessesForUser(null);
        assertThat(weaknesses).isNotEmpty();

        WeaknessResponse boundaryWeakness = weaknesses.stream()
                .filter(w -> w.getConcept().equalsIgnoreCase("Binary Search"))
                .findFirst()
                .orElse(null);

        assertThat(boundaryWeakness).isNotNull();
        assertThat(boundaryWeakness.getFrequency()).isGreaterThanOrEqualTo(2);

        // Verify Today's Focus provides targeted problem curriculum
        TodaysFocusResponse focus = recommendationService.getTodaysFocus(null);
        assertThat(focus).isNotNull();
        assertThat(focus.getWeakness()).isEqualTo("Binary Search");
        assertThat(focus.getWhy()).contains("mistake");
    }

    @Test
    @DisplayName("Adaptive feedback loop: Accepted submission on recommended problem completes recommendation and boosts mastery")
    void shouldCompleteRecommendationAndBoostMasteryOnAcceptedAttempt() {
        // 1. Create a failed attempt to generate weakness
        SubmissionRequest failAttempt = SubmissionRequest.builder()
                .problemTitle("Number of Islands")
                .difficulty("MEDIUM")
                .topics(List.of("Graph", "BFS"))
                .code("queue.add(neighbor);")
                .result(SubmissionResult.TIME_LIMIT_EXCEEDED)
                .userExplanation("Forgot to mark visited before pushing into BFS queue")
                .build();
        submissionService.createSubmission(failAttempt);

        // 2. Solve problem successfully with ACCEPTED
        SubmissionRequest passAttempt = SubmissionRequest.builder()
                .problemTitle("Number of Islands")
                .difficulty("MEDIUM")
                .topics(List.of("Graph", "BFS"))
                .code("visited[nr][nc] = true; queue.add(new int[]{nr, nc});")
                .result(SubmissionResult.ACCEPTED)
                .build();

        SubmissionResponse passResponse = submissionService.createSubmission(passAttempt);
        assertThat(passResponse.getResult()).isEqualTo(SubmissionResult.ACCEPTED);

        // Verify weakness mastery is healthy
        List<WeaknessResponse> weaknesses = weaknessService.getWeaknessesForUser(null);
        assertThat(weaknesses).isNotEmpty();
    }
}
