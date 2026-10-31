package com.codememory.service.impl;

import com.codememory.config.SecurityUtils;
import com.codememory.dto.DashboardSummaryResponse;
import com.codememory.dto.MistakeProfileItemResponse;
import com.codememory.dto.TrendResponse;
import com.codememory.entity.Mistake;
import com.codememory.entity.Submission;
import com.codememory.entity.Weakness;
import com.codememory.entity.enums.MistakeCategory;
import com.codememory.entity.enums.SubmissionResult;
import com.codememory.repository.MistakeRepository;
import com.codememory.repository.SubmissionRepository;
import com.codememory.repository.WeaknessRepository;
import com.codememory.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final SubmissionRepository submissionRepository;
    private final MistakeRepository mistakeRepository;
    private final WeaknessRepository weaknessRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return getSummary(securityUtils.getAuthenticatedUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        Long resolvedUserId = resolveTargetUserId(userId);

        List<Submission> userSubmissions = submissionRepository.findByUserIdOrderBySubmittedAtDesc(resolvedUserId);
        List<Weakness> userWeaknesses = weaknessRepository.findByUserIdOrderByFrequencyDesc(resolvedUserId);
        List<Mistake> allUserMistakes = mistakeRepository.findByUserId(resolvedUserId);

        long problemsAttempted = userSubmissions.stream()
                .map(s -> s.getProblem().getId())
                .distinct()
                .count();

        long problemsAccepted = userSubmissions.stream()
                .filter(s -> s.getResult() == SubmissionResult.ACCEPTED)
                .map(s -> s.getProblem().getId())
                .distinct()
                .count();

        long totalMistakes = allUserMistakes.size();
        long resolvedMistakes = allUserMistakes.stream()
                .filter(m -> m.getStatus() == com.codememory.entity.enums.MistakeStatus.RESOLVED)
                .count();
        long openMistakes = totalMistakes - resolvedMistakes;

        long recurringMistakes = allUserMistakes.stream()
                .filter(m -> m.getStatus() == com.codememory.entity.enums.MistakeStatus.RECURRING)
                .count();

        int recoveryRate = totalMistakes > 0
                ? (int) Math.round(((double) resolvedMistakes / totalMistakes) * 100)
                : 100;

        // Smart Recency-Weighted Mastery Calculation
        int currentMastery = 0;
        String strongestConcept = "—";
        String weakestConcept = "—";
        String learningVerdict;

        if (problemsAttempted > 0) {
            // 1. Recency Decay Window over last 12 attempts
            List<Submission> recentSubmissions = userSubmissions.stream().limit(12).toList();
            double weightedScoreSum = 0.0;
            double weightTotal = 0.0;

            for (int i = 0; i < recentSubmissions.size(); i++) {
                Submission sub = recentSubmissions.get(i);
                // Exponential recency weight: recent index 0 has weight 1.0, 5th has 0.62, 11th has 0.43
                double weight = 1.0 / (1.0 + 0.12 * i);
                double attemptScore;

                if (sub.getResult() == SubmissionResult.ACCEPTED) {
                    attemptScore = 100.0;
                } else {
                    // Check if mistake on this submission was resolved by a subsequent accepted solution
                    List<Mistake> subMistakes = mistakeRepository.findBySubmissionId(sub.getId());
                    boolean wasOvercome = !subMistakes.isEmpty() && subMistakes.stream()
                            .allMatch(m -> m.getStatus() == com.codememory.entity.enums.MistakeStatus.RESOLVED);

                    // If resolved: 75 points (growth mindset: error was acknowledged, learned, and overcome!)
                    // If still open/recurring: 25 points
                    attemptScore = wasOvercome ? 75.0 : 25.0;
                }

                weightedScoreSum += attemptScore * weight;
                weightTotal += weight;
            }

            double recentScore = weightTotal > 0 ? (weightedScoreSum / weightTotal) : 0.0;

            // 2. Blend with concept baseline if weaknesses exist
            if (!userWeaknesses.isEmpty()) {
                double avgConceptScore = userWeaknesses.stream()
                        .mapToDouble(w -> w.getMasteryScore() != null ? w.getMasteryScore() : 50.0)
                        .average()
                        .orElse(50.0);
                // 65% recent performance trajectory + 35% conceptual mastery foundation
                currentMastery = (int) Math.round(recentScore * 0.65 + avgConceptScore * 0.35);
            } else {
                currentMastery = (int) Math.round(recentScore);
            }

            // Bound score cleanly between 10 and 100
            currentMastery = Math.min(100, Math.max(10, currentMastery));

            // Strongest Concept
            if (!userWeaknesses.isEmpty()) {
                strongestConcept = userWeaknesses.stream()
                        .max(Comparator.comparingDouble(w -> w.getMasteryScore() != null ? w.getMasteryScore() : 50.0))
                        .map(Weakness::getConcept)
                        .orElse("—");
            } else {
                strongestConcept = userSubmissions.stream()
                        .filter(s -> s.getResult() == SubmissionResult.ACCEPTED)
                        .findFirst()
                        .map(s -> !s.getProblem().getTopics().isEmpty() ? s.getProblem().getTopics().get(0) : s.getProblem().getTitle())
                        .orElse("—");
            }

            // Identify active focus concept (only consider concepts with ACTIVE open mistakes)
            Optional<Weakness> activeFocus = userWeaknesses.stream()
                    .filter(w -> {
                        List<Mistake> mList = mistakeRepository.findByUserIdAndCategory(resolvedUserId, w.getMistakeCategory());
                        return mList.stream().anyMatch(m -> m.getStatus() != com.codememory.entity.enums.MistakeStatus.RESOLVED);
                    })
                    .min(Comparator.comparingDouble(w -> w.getMasteryScore() != null ? w.getMasteryScore() : 50.0));

            if (activeFocus.isPresent()) {
                weakestConcept = activeFocus.get().getConcept();
            } else if (totalMistakes > 0 && openMistakes == 0) {
                weakestConcept = "All Resolved ✓";
            } else {
                weakestConcept = "None";
            }
        }

        // Generate longitudinal learning verdict
        if (problemsAttempted == 0) {
            learningVerdict = "Awaiting problem attempts to detect cognitive learning patterns.";
        } else if (totalMistakes == 0) {
            learningVerdict = "Consistently clean implementations. No algorithmic failure patterns detected.";
        } else if (recoveryRate >= 75) {
            learningVerdict = String.format("🌟 %d%% Recovery Rate — Excellent learning velocity. You actively eliminate identified failure modes.", recoveryRate);
        } else if (recoveryRate >= 45) {
            learningVerdict = String.format("📈 %d%% Recovery Rate — Steady improvement. Resolving previous algorithmic hurdles.", recoveryRate);
        } else {
            learningVerdict = String.format("⚠️ %d%% Recovery Rate — %d open mistake%s require deliberate practice.",
                    recoveryRate, openMistakes, openMistakes > 1 ? "s" : "");
        }

        return DashboardSummaryResponse.builder()
                .problemsAttempted(problemsAttempted)
                .problemsAccepted(problemsAccepted)
                .totalMistakes(totalMistakes)
                .recurringMistakes(recurringMistakes)
                .resolvedMistakes(resolvedMistakes)
                .openMistakes(openMistakes)
                .recoveryRate(recoveryRate)
                .learningVerdict(learningVerdict)
                .strongestConcept(strongestConcept)
                .weakestConcept(weakestConcept)
                .currentMastery(currentMastery)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrendResponse> getTrends() {
        return getTrends(securityUtils.getAuthenticatedUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrendResponse> getTrends(Long userId) {
        Long resolvedUserId = resolveTargetUserId(userId);
        List<Mistake> mistakes = mistakeRepository.findByUserId(resolvedUserId);

        if (mistakes.isEmpty()) {
            return Collections.emptyList();
        }

        // Group mistakes by YearMonth to compute real longitudinal trends
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        Map<YearMonth, List<Mistake>> byMonth = mistakes.stream()
                .collect(Collectors.groupingBy(m -> {
                    LocalDateTime dt = m.getCreatedAt() != null ? m.getCreatedAt() : LocalDateTime.now();
                    return YearMonth.from(dt);
                }, TreeMap::new, Collectors.toList()));

        List<TrendResponse> trends = new ArrayList<>();
        for (Map.Entry<YearMonth, List<Mistake>> entry : byMonth.entrySet()) {
            List<Mistake> monthMistakes = entry.getValue();
            long total = monthMistakes.size();
            long boundary = monthMistakes.stream()
                    .filter(m -> m.getCategory() == MistakeCategory.BOUNDARY_ERROR || m.getCategory() == MistakeCategory.OFF_BY_ONE_ERROR)
                    .count();
            long complexity = monthMistakes.stream()
                    .filter(m -> m.getCategory() == MistakeCategory.COMPLEXITY_ERROR)
                    .count();
            long edgeCase = monthMistakes.stream()
                    .filter(m -> m.getCategory() == MistakeCategory.EDGE_CASE_ERROR || m.getCategory() == MistakeCategory.NULL_OR_EMPTY_INPUT_ERROR)
                    .count();

            trends.add(TrendResponse.builder()
                    .period(entry.getKey().format(displayFormatter))
                    .boundary(boundary)
                    .complexity(complexity)
                    .edgeCase(edgeCase)
                    .total(total)
                    .build());
        }

        return trends;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MistakeProfileItemResponse> getMistakeProfile() {
        return getMistakeProfile(securityUtils.getAuthenticatedUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MistakeProfileItemResponse> getMistakeProfile(Long userId) {
        Long resolvedUserId = resolveTargetUserId(userId);
        List<Mistake> mistakes = mistakeRepository.findByUserId(resolvedUserId);

        if (mistakes.isEmpty()) {
            return Collections.emptyList();
        }

        long total = mistakes.size();
        Map<MistakeCategory, Long> counts = mistakes.stream()
                .collect(Collectors.groupingBy(Mistake::getCategory, Collectors.counting()));

        String[] palette = {"#f43f5e", "#f59e0b", "#a855f7", "#06b6d4", "#818cf8", "#10b981"};
        int colorIdx = 0;

        List<MistakeProfileItemResponse> result = new ArrayList<>();
        for (Map.Entry<MistakeCategory, Long> entry : counts.entrySet()) {
            int pct = (int) Math.round(((double) entry.getValue() / total) * 100);
            String label = formatCategoryLabel(entry.getKey());
            result.add(MistakeProfileItemResponse.builder()
                    .category(entry.getKey())
                    .label(label)
                    .count(entry.getValue())
                    .percentage(pct)
                    .color(palette[colorIdx % palette.length])
                    .build());
            colorIdx++;
        }

        result.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        return result;
    }

    private Long resolveTargetUserId(Long userId) {
        Long authUserId = securityUtils.getAuthenticatedUserId();
        // Strict isolation: if a client requests a specific userId, it must match the authenticated principal
        if (userId != null && !userId.equals(authUserId)) {
            log.warn("Cross-user access blocked: Authenticated user #{} requested data for user #{}", authUserId, userId);
            return authUserId; // Never allow accessing another user's private data
        }
        return authUserId;
    }

    private String formatCategoryLabel(MistakeCategory category) {
        String name = category.name().replace('_', ' ');
        return Arrays.stream(name.split(" "))
                .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
