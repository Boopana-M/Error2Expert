package com.codememory.service.impl;

import com.codememory.dto.RecommendationResponse;
import com.codememory.dto.TodaysFocusResponse;
import com.codememory.entity.Problem;
import com.codememory.entity.Recommendation;
import com.codememory.entity.User;
import com.codememory.entity.Weakness;
import com.codememory.entity.enums.RecommendationStatus;
import com.codememory.entity.enums.SubmissionResult;
import com.codememory.exception.ResourceNotFoundException;
import com.codememory.repository.ProblemRepository;
import com.codememory.repository.RecommendationRepository;
import com.codememory.repository.SubmissionRepository;
import com.codememory.repository.WeaknessRepository;
import com.codememory.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final ProblemRepository problemRepository;
    private final WeaknessRepository weaknessRepository;
    private final SubmissionRepository submissionRepository;
    private final com.codememory.repository.UserRepository userRepository;

    public RecommendationServiceImpl(
            RecommendationRepository recommendationRepository,
            ProblemRepository problemRepository,
            WeaknessRepository weaknessRepository,
            SubmissionRepository submissionRepository,
            com.codememory.repository.UserRepository userRepository) {
        this.recommendationRepository = recommendationRepository;
        this.problemRepository = problemRepository;
        this.weaknessRepository = weaknessRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsForUser(Long userId) {
        Long targetUserId = (userId != null) ? userId : resolveDefaultUserId();
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(targetUserId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TodaysFocusResponse getTodaysFocus(Long userId) {
        Long targetUserId = (userId != null) ? userId : resolveDefaultUserId();
        List<Weakness> weaknesses = weaknessRepository.findByUserIdOrderByFrequencyDesc(targetUserId);

        if (weaknesses.isEmpty()) {
            return TodaysFocusResponse.builder()
                    .weakness("General Algorithmic Foundations")
                    .why("No high-frequency mistakes detected yet. Continue practicing standard LeetCode problems.")
                    .recommendedProblems(List.of())
                    .build();
        }

        // Top recurring weakness
        Weakness topWeakness = weaknesses.get(0);

        List<RecommendationResponse> recommendedProblems = recommendationRepository
                .findByUserIdAndStatus(targetUserId, RecommendationStatus.PENDING)
                .stream()
                .filter(r -> r.getWeakness() != null && r.getWeakness().getId().equals(topWeakness.getId()))
                .map(this::mapToResponse)
                .toList();

        // If no recommendations currently active for top weakness, generate them
        if (recommendedProblems.isEmpty()) {
            recommendedProblems = generateFallbackRecommendations(topWeakness);
        }

        String whyExplanation = String.format(
                "You have made %d %s mistake%s across %s problems. Your current mastery is %.0f%%. " +
                "Targeted practice is recommended to eliminate this specific failure mode.",
                topWeakness.getFrequency(),
                topWeakness.getMistakeCategory().name().replace('_', ' ').toLowerCase(),
                topWeakness.getFrequency() > 1 ? "s" : "",
                topWeakness.getConcept(),
                topWeakness.getMasteryScore() != null ? topWeakness.getMasteryScore() : 70.0
        );

        return TodaysFocusResponse.builder()
                .weakness(topWeakness.getConcept())
                .why(whyExplanation)
                .recommendedProblems(recommendedProblems)
                .build();
    }

    @Override
    @Transactional
    public void generateRecommendationsForWeakness(Weakness weakness) {
        if (weakness == null || weakness.getUser() == null) return;

        User user = weakness.getUser();
        String concept = weakness.getConcept();

        // Find candidate problems by matching topic or title
        List<Problem> candidates = problemRepository.findAll().stream()
                .filter(p -> p.getTopics().stream().anyMatch(t -> t.equalsIgnoreCase(concept)) ||
                        p.getTitle().toLowerCase().contains(concept.toLowerCase()))
                .toList();

        for (Problem problem : candidates) {
            // Check if already solved with ACCEPTED
            boolean alreadySolved = submissionRepository
                    .findByUserIdAndProblemIdOrderBySubmittedAtAsc(user.getId(), problem.getId())
                    .stream()
                    .anyMatch(s -> s.getResult() == SubmissionResult.ACCEPTED);

            if (alreadySolved) continue;

            // Check if recommendation already exists
            boolean alreadyRecommended = recommendationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                    .stream()
                    .anyMatch(r -> r.getProblem().getId().equals(problem.getId()) &&
                            r.getStatus() == RecommendationStatus.PENDING);

            if (alreadyRecommended) continue;

            String reason = String.format("Targeted practice to reinforce '%s' and prevent %s recurrence.",
                    concept, weakness.getMistakeCategory().name().replace('_', ' '));

            Recommendation recommendation = Recommendation.builder()
                    .user(user)
                    .weakness(weakness)
                    .problem(problem)
                    .reason(reason)
                    .status(RecommendationStatus.PENDING)
                    .build();

            recommendationRepository.save(recommendation);
            log.info("Generated adaptive recommendation for user '{}': Problem '{}'", user.getName(), problem.getTitle());
        }
    }

    @Override
    @Transactional
    public void processSolvedProblemFeedback(User user, Problem problem) {
        if (user == null || problem == null) return;

        // 1. Resolve pending recommendations for this problem
        List<Recommendation> pending = recommendationRepository.findByUserIdAndStatus(user.getId(), RecommendationStatus.PENDING)
                .stream()
                .filter(r -> r.getProblem().getId().equals(problem.getId()))
                .toList();

        for (Recommendation rec : pending) {
            rec.setStatus(RecommendationStatus.SOLVED);
            recommendationRepository.save(rec);
            log.info("Adaptive feedback loop: Marked recommendation #{} as SOLVED for problem '{}'", rec.getId(), problem.getTitle());

            // Boost weakness mastery score
            if (rec.getWeakness() != null) {
                Weakness weakness = rec.getWeakness();
                double currentMastery = weakness.getMasteryScore() != null ? weakness.getMasteryScore() : 70.0;
                double newMastery = Math.min(100.0, currentMastery + 8.0);
                weakness.setMasteryScore(newMastery);
                weaknessRepository.save(weakness);
                log.info("Longitudinal mastery updated for weakness '{}': {}% -> {}%", weakness.getConcept(), currentMastery, newMastery);
            }
        }
    }

    @Override
    @Transactional
    public RecommendationResponse updateRecommendationStatus(Long recommendationId, RecommendationStatus status) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with ID: " + recommendationId));

        recommendation.setStatus(status);
        Recommendation saved = recommendationRepository.save(recommendation);
        return mapToResponse(saved);
    }

    private List<RecommendationResponse> generateFallbackRecommendations(Weakness weakness) {
        return problemRepository.findAll().stream()
                .limit(3)
                .map(p -> RecommendationResponse.builder()
                        .weaknessId(weakness.getId())
                        .weaknessConcept(weakness.getConcept())
                        .problemId(p.getId())
                        .leetcodeId(p.getLeetcodeId())
                        .title(p.getTitle())
                        .difficulty(p.getDifficulty())
                        .topics(p.getTopics())
                        .url(p.getUrl())
                        .reason("Reinforce proper algorithm boundaries and state maintenance.")
                        .status(RecommendationStatus.PENDING)
                        .build())
                .toList();
    }

    private RecommendationResponse mapToResponse(Recommendation rec) {
        return RecommendationResponse.builder()
                .id(rec.getId())
                .weaknessId(rec.getWeakness() != null ? rec.getWeakness().getId() : null)
                .weaknessConcept(rec.getWeakness() != null ? rec.getWeakness().getConcept() : "General")
                .problemId(rec.getProblem().getId())
                .leetcodeId(rec.getProblem().getLeetcodeId())
                .title(rec.getProblem().getTitle())
                .difficulty(rec.getProblem().getDifficulty())
                .topics(rec.getProblem().getTopics())
                .url(rec.getProblem().getUrl())
                .reason(rec.getReason())
                .status(rec.getStatus())
                .createdAt(rec.getCreatedAt())
                .build();
    }

    private Long resolveDefaultUserId() {
        return userRepository.findAll().stream().findFirst().map(u -> u.getId()).orElse(1L);
    }
}
