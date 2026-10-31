package com.codememory.service.impl;

import com.codememory.config.SecurityUtils;
import com.codememory.dto.RecommendationResponse;
import com.codememory.dto.TodaysFocusResponse;
import com.codememory.entity.Mistake;
import com.codememory.entity.Problem;
import com.codememory.entity.Recommendation;
import com.codememory.entity.User;
import com.codememory.entity.Weakness;
import com.codememory.entity.enums.RecommendationStatus;
import com.codememory.entity.enums.SubmissionResult;
import com.codememory.exception.ResourceNotFoundException;
import com.codememory.repository.MistakeRepository;
import com.codememory.repository.ProblemRepository;
import com.codememory.repository.RecommendationRepository;
import com.codememory.repository.SubmissionRepository;
import com.codememory.repository.WeaknessRepository;
import com.codememory.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final ProblemRepository problemRepository;
    private final WeaknessRepository weaknessRepository;
    private final SubmissionRepository submissionRepository;
    private final MistakeRepository mistakeRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsForCurrentUser() {
        return getRecommendationsForUser(securityUtils.getAuthenticatedUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsForUser(Long userId) {
        Long currentUserId = securityUtils.getAuthenticatedUserId();
        Long targetUserId = (userId != null && userId.equals(currentUserId)) ? userId : currentUserId;

        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(targetUserId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TodaysFocusResponse getTodaysFocusForCurrentUser() {
        return getTodaysFocus(securityUtils.getAuthenticatedUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public TodaysFocusResponse getTodaysFocus(Long userId) {
        Long currentUserId = securityUtils.getAuthenticatedUserId();
        Long targetUserId = (userId != null && userId.equals(currentUserId)) ? userId : currentUserId;

        List<Weakness> weaknesses = weaknessRepository.findByUserIdOrderByFrequencyDesc(targetUserId);

        if (weaknesses.isEmpty()) {
            return TodaysFocusResponse.builder()
                    .weakness("—")
                    .why("No recurring error patterns detected yet. Complete LeetCode problem attempts to trigger personalized practice recommendations.")
                    .recommendedProblems(List.of())
                    .build();
        }

        // Find top focus: prioritize weakness with active open mistakes
        Weakness topWeakness = weaknesses.stream()
                .filter(w -> {
                    List<Mistake> mList = mistakeRepository.findByUserIdAndCategory(targetUserId, w.getMistakeCategory());
                    return mList.stream().anyMatch(m -> m.getStatus() != com.codememory.entity.enums.MistakeStatus.RESOLVED);
                })
                .findFirst()
                .orElse(weaknesses.get(0));

        List<RecommendationResponse> recommendedProblems = recommendationRepository
                .findByUserIdAndStatus(targetUserId, RecommendationStatus.PENDING)
                .stream()
                .filter(r -> r.getWeakness() != null && r.getWeakness().getId().equals(topWeakness.getId()))
                .map(this::mapToResponse)
                .toList();

        List<Mistake> catMistakes = mistakeRepository.findByUserIdAndCategory(targetUserId, topWeakness.getMistakeCategory());
        long resolved = catMistakes.stream().filter(m -> m.getStatus() == com.codememory.entity.enums.MistakeStatus.RESOLVED).count();
        long total = catMistakes.size();
        int recRate = total > 0 ? (int) Math.round(((double) resolved / total) * 100) : 100;
        long openCount = total - resolved;

        String whyExplanation;
        if (total > 0 && openCount == 0) {
            whyExplanation = String.format("All %d past %s mistakes in %s have been successfully resolved (100%% recovery). Mastery is robust at %.0f%%.",
                    total, topWeakness.getMistakeCategory().name().replace('_', ' ').toLowerCase(), topWeakness.getConcept(),
                    topWeakness.getMasteryScore() != null ? topWeakness.getMasteryScore() : 90.0);
        } else {
            whyExplanation = String.format("You have %d open mistake%s in %s (Recovery Rate: %d%%, Mastery: %.0f%%). Targeted practice is calibrated to overcome this specific failure mode.",
                    openCount, openCount != 1 ? "s" : "", topWeakness.getConcept(), recRate,
                    topWeakness.getMasteryScore() != null ? topWeakness.getMasteryScore() : 50.0);
        }

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

        // Find candidate problems by matching topic or title from shared catalog
        List<Problem> candidates = problemRepository.findAll().stream()
                .filter(p -> p.getTopics().stream().anyMatch(t -> t.equalsIgnoreCase(concept)) ||
                        p.getTitle().toLowerCase().contains(concept.toLowerCase()))
                .toList();

        for (Problem problem : candidates) {
            // Check if user already solved with ACCEPTED
            boolean alreadySolved = submissionRepository
                    .findByUserIdAndProblemIdOrderBySubmittedAtAsc(user.getId(), problem.getId())
                    .stream()
                    .anyMatch(s -> s.getResult() == SubmissionResult.ACCEPTED);

            if (alreadySolved) continue;

            // Check if recommendation already exists for this user
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
                double currentMastery = weakness.getMasteryScore() != null ? weakness.getMasteryScore() : 50.0;
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

        Long currentUserId = securityUtils.getAuthenticatedUserId();
        if (!recommendation.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Recommendation not found with ID: " + recommendationId);
        }

        recommendation.setStatus(status);
        Recommendation saved = recommendationRepository.save(recommendation);
        return mapToResponse(saved);
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
}
