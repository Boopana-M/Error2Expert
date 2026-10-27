package com.codememory.config;

import com.codememory.entity.*;
import com.codememory.entity.enums.MistakeCategory;
import com.codememory.entity.enums.RecommendationStatus;
import com.codememory.entity.enums.SubmissionResult;
import com.codememory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Production Data Initializer.
 * Automatically bootstraps core problem catalogs, demo account, and
 * realistic longitudinal weakness datasets if running on a fresh cloud database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final MistakeRepository mistakeRepository;
    private final WeaknessRepository weaknessRepository;
    private final RecommendationRepository recommendationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Running DataInitializer database check...");

        // 1. Seed Problems Catalog
        if (problemRepository.count() == 0) {
            log.info("No problems found in database. Seeding canonical LeetCode catalog...");
            seedProblems();
        }

        // 2. Seed Default User
        User demoUser = userRepository.findByEmail("boopana@example.com").orElse(null);
        if (demoUser == null && userRepository.count() == 0) {
            log.info("No users found in database. Seeding demo user 'Boopana'...");
            demoUser = User.builder()
                    .name("Boopana")
                    .email("boopana@example.com")
                    .passwordHash(passwordEncoder.encode("password"))
                    .build();
            demoUser = userRepository.save(demoUser);
        } else if (demoUser == null) {
            demoUser = userRepository.findAll().get(0);
        }

        // 3. Seed Initial Longitudinal Mistake History
        if (submissionRepository.count() == 0 && demoUser != null) {
            log.info("No submissions found. Seeding initial longitudinal mistake history...");
            seedHistoricalData(demoUser);
        }

        log.info("DataInitializer completed. Database is ready with {} problems and {} submissions.",
                problemRepository.count(), submissionRepository.count());
    }

    private void seedProblems() {
        List<Problem> catalog = List.of(
                Problem.builder().leetcodeId("704").title("Binary Search").difficulty("EASY")
                        .url("https://leetcode.com/problems/binary-search/")
                        .topics(new ArrayList<>(List.of("Binary Search", "Array"))).build(),
                Problem.builder().leetcodeId("33").title("Search in Rotated Sorted Array").difficulty("MEDIUM")
                        .url("https://leetcode.com/problems/search-in-rotated-sorted-array/")
                        .topics(new ArrayList<>(List.of("Binary Search", "Array"))).build(),
                Problem.builder().leetcodeId("153").title("Find Minimum in Rotated Sorted Array").difficulty("MEDIUM")
                        .url("https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/")
                        .topics(new ArrayList<>(List.of("Binary Search", "Array"))).build(),
                Problem.builder().leetcodeId("35").title("Search Insert Position").difficulty("EASY")
                        .url("https://leetcode.com/problems/search-insert-position/")
                        .topics(new ArrayList<>(List.of("Binary Search", "Array"))).build(),
                Problem.builder().leetcodeId("200").title("Number of Islands").difficulty("MEDIUM")
                        .url("https://leetcode.com/problems/number-of-islands/")
                        .topics(new ArrayList<>(List.of("Depth-First Search", "Breadth-First Search", "Graph", "Matrix"))).build(),
                Problem.builder().leetcodeId("133").title("Clone Graph").difficulty("MEDIUM")
                        .url("https://leetcode.com/problems/clone-graph/")
                        .topics(new ArrayList<>(List.of("Depth-First Search", "Breadth-First Search", "Graph"))).build(),
                Problem.builder().leetcodeId("207").title("Course Schedule").difficulty("MEDIUM")
                        .url("https://leetcode.com/problems/course-schedule/")
                        .topics(new ArrayList<>(List.of("Depth-First Search", "Breadth-First Search", "Graph", "Topological Sort"))).build(),
                Problem.builder().leetcodeId("261").title("Graph Valid Tree").difficulty("MEDIUM")
                        .url("https://leetcode.com/problems/graph-valid-tree/")
                        .topics(new ArrayList<>(List.of("Depth-First Search", "Breadth-First Search", "Graph", "Union Find"))).build(),
                Problem.builder().leetcodeId("322").title("Coin Change").difficulty("MEDIUM")
                        .url("https://leetcode.com/problems/coin-change/")
                        .topics(new ArrayList<>(List.of("Dynamic Programming", "Breadth-First Search"))).build(),
                Problem.builder().leetcodeId("70").title("Climbing Stairs").difficulty("EASY")
                        .url("https://leetcode.com/problems/climbing-stairs/")
                        .topics(new ArrayList<>(List.of("Dynamic Programming", "Math"))).build()
        );
        problemRepository.saveAll(catalog);
    }

    private void seedHistoricalData(User user) {
        Problem pBinarySearch = problemRepository.findByTitleIgnoreCase("Binary Search").orElse(null);
        Problem pRotated = problemRepository.findByTitleIgnoreCase("Search in Rotated Sorted Array").orElse(null);
        Problem pMinRotated = problemRepository.findByTitleIgnoreCase("Find Minimum in Rotated Sorted Array").orElse(null);
        Problem pSearchInsert = problemRepository.findByTitleIgnoreCase("Search Insert Position").orElse(null);
        Problem pIslands = problemRepository.findByTitleIgnoreCase("Number of Islands").orElse(null);
        Problem pCourseSchedule = problemRepository.findByTitleIgnoreCase("Course Schedule").orElse(null);
        Problem pGraphTree = problemRepository.findByTitleIgnoreCase("Graph Valid Tree").orElse(null);

        if (pBinarySearch == null || pRotated == null) return;

        LocalDateTime now = LocalDateTime.now();

        // Submissions & Mistakes
        Submission sub1 = submissionRepository.save(Submission.builder()
                .user(user).problem(pBinarySearch).language("java")
                .result(SubmissionResult.TIME_LIMIT_EXCEEDED).attemptNumber(1)
                .code("class Solution { public int search(int[] nums, int target) { int l=0, r=nums.length; while(l<r) { int m=(l+r)/2; if(nums[m]==target) return m; if(nums[m]<target) l=m; else r=m; } return -1; } }")
                .submittedAt(now.minusDays(5)).build());

        mistakeRepository.save(Mistake.builder()
                .submission(sub1).category(MistakeCategory.BOUNDARY_ERROR)
                .userExplanation("I used l=m instead of l=m+1 and loop never terminated on boundary.")
                .aiExplanation("Infinite loop caused by improper low pointer advance during binary search.")
                .confidence(0.94).createdAt(now.minusDays(5)).build());

        submissionRepository.save(Submission.builder()
                .user(user).problem(pBinarySearch).language("java")
                .result(SubmissionResult.ACCEPTED).attemptNumber(2)
                .code("class Solution { public int search(int[] nums, int target) { int l=0, r=nums.length-1; while(l<=r) { int m=(l+r)/2; if(nums[m]==target) return m; if(nums[m]<target) l=m+1; else r=m-1; } return -1; } }")
                .submittedAt(now.minusDays(5)).build());

        Submission sub3 = submissionRepository.save(Submission.builder()
                .user(user).problem(pRotated).language("java")
                .result(SubmissionResult.WRONG_ANSWER).attemptNumber(1)
                .code("class Solution { public int search(int[] nums, int target) { int low=0, high=nums.length-1; while(low<=high) { int mid=low+(high-low)/2; if(nums[mid]==target) return mid; if(nums[low]<=nums[mid]) { if(target>=nums[low] && target<nums[mid]) high=mid; else low=mid; } } return -1; } }")
                .submittedAt(now.minusDays(4)).build());

        mistakeRepository.save(Mistake.builder()
                .submission(sub3).category(MistakeCategory.BOUNDARY_ERROR)
                .userExplanation("I set high=mid instead of high=mid-1, got stuck at boundaries.")
                .aiExplanation("Incorrect binary search boundary management: interval updates omit +/- 1 offset.")
                .confidence(0.92).createdAt(now.minusDays(4)).build());

        if (pMinRotated != null) {
            Submission sub4 = submissionRepository.save(Submission.builder()
                    .user(user).problem(pMinRotated).language("java")
                    .result(SubmissionResult.WRONG_ANSWER).attemptNumber(1)
                    .code("class Solution { public int findMin(int[] nums) { int left=0, right=nums.length-1; while(left<right) { int mid=left+(right-left)/2; if(nums[mid]>nums[right]) left=mid; else right=mid-1; } return nums[left]; } }")
                    .submittedAt(now.minusDays(3)).build());

            mistakeRepository.save(Mistake.builder()
                    .submission(sub4).category(MistakeCategory.BOUNDARY_ERROR)
                    .userExplanation("I subtracted 1 from right when mid could be the minimum.")
                    .aiExplanation("Premature elimination of potential answer by mismanaging binary search boundary.")
                    .confidence(0.89).createdAt(now.minusDays(3)).build());
        }

        if (pIslands != null) {
            Submission sub5 = submissionRepository.save(Submission.builder()
                    .user(user).problem(pIslands).language("java")
                    .result(SubmissionResult.RUNTIME_ERROR).attemptNumber(1)
                    .code("class Solution { public int numIslands(char[][] grid) { int count=0; ... } }")
                    .submittedAt(now.minusDays(2)).build());

            mistakeRepository.save(Mistake.builder()
                    .submission(sub5).category(MistakeCategory.GRAPH_VISITED_ERROR)
                    .userExplanation("Forgot to mark visited in DFS so stack overflowed.")
                    .aiExplanation("Missing visited-state transition in graph traversal leading to infinite recursion.")
                    .confidence(0.96).createdAt(now.minusDays(2)).build());
        }

        if (pCourseSchedule != null) {
            Submission sub6 = submissionRepository.save(Submission.builder()
                    .user(user).problem(pCourseSchedule).language("java")
                    .result(SubmissionResult.WRONG_ANSWER).attemptNumber(1)
                    .code("class Solution { public boolean canFinish(int numCourses, int[][] prerequisites) { ... } }")
                    .submittedAt(now.minusDays(1)).build());

            mistakeRepository.save(Mistake.builder()
                    .submission(sub6).category(MistakeCategory.GRAPH_VISITED_ERROR)
                    .userExplanation("Did not unmark visited or track path for cycle detection in directed graph.")
                    .aiExplanation("Confusion between visited global state and recursion stack cycle state in DAG traversal.")
                    .confidence(0.90).createdAt(now.minusDays(1)).build());
        }

        // Longitudinal Weaknesses
        Weakness wBoundary = weaknessRepository.save(Weakness.builder()
                .user(user).concept("Binary Search").mistakeCategory(MistakeCategory.BOUNDARY_ERROR)
                .frequency(3).confidence(0.92).masteryScore(72.0)
                .firstObserved(now.minusDays(5)).lastObserved(now.minusDays(3)).build());

        Weakness wGraph = weaknessRepository.save(Weakness.builder()
                .user(user).concept("Graph Traversal").mistakeCategory(MistakeCategory.GRAPH_VISITED_ERROR)
                .frequency(2).confidence(0.93).masteryScore(78.5)
                .firstObserved(now.minusDays(2)).lastObserved(now.minusDays(1)).build());

        // Recommendations
        if (pSearchInsert != null) {
            recommendationRepository.save(Recommendation.builder()
                    .user(user).weakness(wBoundary).problem(pSearchInsert)
                    .reason("Practice standard boundary condition termination with Search Insert Position (Easy)")
                    .status(RecommendationStatus.PENDING).createdAt(now.minusDays(3)).build());
        }
        if (pRotated != null) {
            recommendationRepository.save(Recommendation.builder()
                    .user(user).weakness(wBoundary).problem(pRotated)
                    .reason("Re-attempt Search in Rotated Sorted Array focusing on left/right boundary updates")
                    .status(RecommendationStatus.PENDING).createdAt(now.minusDays(3)).build());
        }
        if (pGraphTree != null) {
            recommendationRepository.save(Recommendation.builder()
                    .user(user).weakness(wGraph).problem(pGraphTree)
                    .reason("Strengthen visited state tracking and cycle detection with Graph Valid Tree (Medium)")
                    .status(RecommendationStatus.PENDING).createdAt(now.minusDays(1)).build());
        }
    }
}
