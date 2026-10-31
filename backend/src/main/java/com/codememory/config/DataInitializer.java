package com.codememory.config;

import com.codememory.entity.Problem;
import com.codememory.entity.Submission;
import com.codememory.entity.Weakness;
import com.codememory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Production Data Initializer.
 * Seeds ONLY global static reference data (canonical LeetCode problem catalog).
 * Does NOT seed demo users, demo submissions, demo weaknesses, or demo history.
 * Ensures strict multi-user isolation so newly registered users start completely empty.
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

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Running DataInitializer database check...");

        // 1. Seed Problems Catalog (Shared global reference data)
        if (problemRepository.count() == 0) {
            log.info("No problems found in database. Seeding canonical LeetCode catalog...");
            seedProblems();
        }

        // 2. Comprehensive cleanup: Purge all legacy seed records across the database
        userRepository.findByEmail("boopana@example.com").ifPresent(userRepository::delete);

        // Reset any existing accounts that carry legacy seed data
        userRepository.findAll().forEach(user -> {
            List<Weakness> userWeaknesses = weaknessRepository.findByUserIdOrderByFrequencyDesc(user.getId());
            boolean isLegacySeeded = userWeaknesses.stream()
                    .anyMatch(w -> ("Binary Search".equals(w.getConcept()) && Double.valueOf(72.0).equals(w.getMasteryScore()))
                                || ("Graph Traversal".equals(w.getConcept()) && Double.valueOf(78.5).equals(w.getMasteryScore())));

            if (isLegacySeeded || "boopana608@gmail.com".equalsIgnoreCase(user.getEmail())) {
                log.info("Resetting legacy seeded history for user '{}' (ID: {})...", user.getEmail(), user.getId());
                recommendationRepository.deleteAll(recommendationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
                weaknessRepository.deleteAll(userWeaknesses);
                List<Submission> subs = submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId());
                for (Submission sub : subs) {
                    mistakeRepository.deleteAll(mistakeRepository.findBySubmissionId(sub.getId()));
                }
                submissionRepository.deleteAll(subs);
                log.info("User '{}' dashboard reset to completely empty.", user.getEmail());
            }
        });

        log.info("DataInitializer completed. Canonical problem catalog contains {} problems.", problemRepository.count());
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
}
