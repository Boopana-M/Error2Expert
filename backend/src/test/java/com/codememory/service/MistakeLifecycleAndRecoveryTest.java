package com.codememory.service;

import com.codememory.dto.DashboardSummaryResponse;
import com.codememory.dto.SubmissionRequest;
import com.codememory.dto.SubmissionResponse;
import com.codememory.dto.WeaknessResponse;
import com.codememory.entity.Mistake;
import com.codememory.entity.User;
import com.codememory.entity.enums.MistakeStatus;
import com.codememory.entity.enums.SubmissionResult;
import com.codememory.repository.MistakeRepository;
import com.codememory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MistakeLifecycleAndRecoveryTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private WeaknessService weaknessService;

    @Autowired
    private MistakeRepository mistakeRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .name("Lifecycle Tester")
                .email("lifecycle@example.com")
                .passwordHash("hashedpass")
                .build());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testUser.getEmail(), null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Mistake lifecycle: OPEN on failure -> RESOLVED on accept -> 100% Recovery Rate -> RECURRING on subsequent relapse")
    void testMistakeLifecycleAndRecovery() {
        // Step 1: Failed submission with boundary error -> Should be OPEN
        SubmissionRequest failReq = SubmissionRequest.builder()
                .problemTitle("Binary Search")
                .leetcodeId("704")
                .difficulty("EASY")
                .topics(List.of("Binary Search", "Array"))
                .language("java")
                .result(SubmissionResult.WRONG_ANSWER)
                .code("int mid = (l+r)/2; if (arr[mid] < target) l = mid;")
                .userExplanation("Boundary error: used l = mid causing infinite loop on boundary condition")
                .build();

        SubmissionResponse failResp = submissionService.createSubmission(failReq);
        assertThat(failResp).isNotNull();
        assertThat(failResp.getMistake()).isNotNull();
        assertThat(failResp.getMistake().getStatus()).isEqualTo(MistakeStatus.OPEN);

        List<Mistake> userMistakes = mistakeRepository.findByUserId(testUser.getId());
        assertThat(userMistakes).hasSize(1);
        Mistake firstMistake = userMistakes.get(0);
        assertThat(firstMistake.getStatus()).isEqualTo(MistakeStatus.OPEN);
        assertThat(firstMistake.getResolvedBySubmission()).isNull();

        // Check Dashboard when mistake is open
        DashboardSummaryResponse midSummary = dashboardService.getSummary();
        assertThat(midSummary.getTotalMistakes()).isEqualTo(1);
        assertThat(midSummary.getOpenMistakes()).isEqualTo(1);
        assertThat(midSummary.getResolvedMistakes()).isEqualTo(0);
        assertThat(midSummary.getRecoveryRate()).isEqualTo(0);

        // Step 2: User learns and submits ACCEPTED solution on the same problem
        SubmissionRequest solveReq = SubmissionRequest.builder()
                .problemTitle("Binary Search")
                .leetcodeId("704")
                .difficulty("EASY")
                .topics(List.of("Binary Search", "Array"))
                .language("java")
                .result(SubmissionResult.ACCEPTED)
                .code("int mid = l + (r - l)/2; if (arr[mid] < target) l = mid + 1;")
                .build();

        SubmissionResponse solveResp = submissionService.createSubmission(solveReq);
        assertThat(solveResp.getResult()).isEqualTo(SubmissionResult.ACCEPTED);

        // Verify the original mistake is now marked RESOLVED!
        Mistake resolvedMistake = mistakeRepository.findById(firstMistake.getId()).orElseThrow();
        assertThat(resolvedMistake.getStatus()).isEqualTo(MistakeStatus.RESOLVED);
        assertThat(resolvedMistake.getResolvedBySubmission()).isNotNull();
        assertThat(resolvedMistake.getResolvedBySubmission().getId()).isEqualTo(solveResp.getId());
        assertThat(resolvedMistake.getResolvedAt()).isNotNull();

        // Check Dashboard after resolution: Recovery Rate = 100%!
        DashboardSummaryResponse recoveredSummary = dashboardService.getSummary();
        assertThat(recoveredSummary.getTotalMistakes()).isEqualTo(1);
        assertThat(recoveredSummary.getResolvedMistakes()).isEqualTo(1);
        assertThat(recoveredSummary.getOpenMistakes()).isEqualTo(0);
        assertThat(recoveredSummary.getRecoveryRate()).isEqualTo(100);
        assertThat(recoveredSummary.getWeakestConcept()).isEqualTo("All Resolved ✓");
        assertThat(recoveredSummary.getLearningVerdict()).contains("100% Recovery Rate");

        // Verify Weakness response reflects "IMPROVING" state with 100% recovery
        List<WeaknessResponse> weaknesses = weaknessService.getWeaknessesForCurrentUser();
        assertThat(weaknesses).isNotEmpty();
        WeaknessResponse conceptWeakness = weaknesses.get(0);
        assertThat(conceptWeakness.getRecoveryRate()).isEqualTo(100);
        assertThat(conceptWeakness.getResolvedCount()).isEqualTo(1);
        assertThat(conceptWeakness.getOpenCount()).isEqualTo(0);
        assertThat(conceptWeakness.getLearningState()).isEqualTo("IMPROVING");
        assertThat(conceptWeakness.getLearningNarrative()).contains("successfully corrected them in recent attempts");

        // Step 3: User makes the same mistake again on a later problem -> Should be marked RECURRING
        SubmissionRequest repeatReq = SubmissionRequest.builder()
                .problemTitle("Search in Rotated Sorted Array")
                .leetcodeId("33")
                .difficulty("MEDIUM")
                .topics(List.of("Binary Search", "Array"))
                .language("java")
                .result(SubmissionResult.WRONG_ANSWER)
                .code("while (l < r) { int mid = (l+r)/2; r = mid; }")
                .userExplanation("Boundary error: used r = mid again")
                .build();

        SubmissionResponse repeatResp = submissionService.createSubmission(repeatReq);
        assertThat(repeatResp.getMistake()).isNotNull();
        assertThat(repeatResp.getMistake().getStatus()).isEqualTo(MistakeStatus.RECURRING);

        // Dashboard now reflects recurring mistake
        DashboardSummaryResponse recurringSummary = dashboardService.getSummary();
        assertThat(recurringSummary.getTotalMistakes()).isEqualTo(2);
        assertThat(recurringSummary.getResolvedMistakes()).isEqualTo(1);
        assertThat(recurringSummary.getOpenMistakes()).isEqualTo(1);
        assertThat(recurringSummary.getRecoveryRate()).isEqualTo(50);
        assertThat(recurringSummary.getRecurringMistakes()).isGreaterThanOrEqualTo(1);
    }
}
