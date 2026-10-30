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

        long problemsAttempted = userSubmissions.stream()
                .map(s -> s.getProblem().getId())
                .distinct()
                .count();

        long problemsAccepted = userSubmissions.stream()
                .filter(s -> s.getResult() == SubmissionResult.ACCEPTED)
                .map(s -> s.getProblem().getId())
                .distinct()
                .count();

        long totalMistakes = userSubmissions.stream()
                .mapToLong(s -> mistakeRepository.findBySubmissionId(s.getId()).size())
                .sum();

        long recurringMistakes = userWeaknesses.stream()
                .filter(w -> w.getFrequency() > 1)
                .mapToLong(Weakness::getFrequency)
                .sum();

        // Fresh user defaults: strict 0 and "—"
        String weakestConcept = "—";
        String strongestConcept = "—";
        int currentMastery = 0;

        if (!userWeaknesses.isEmpty()) {
            weakestConcept = userWeaknesses.get(0).getConcept();

            // Find concept with highest mastery score
            strongestConcept = userWeaknesses.stream()
                    .max(Comparator.comparingDouble(w -> w.getMasteryScore() != null ? w.getMasteryScore() : 50.0))
                    .map(Weakness::getConcept)
                    .orElse("—");

            double avgMastery = userWeaknesses.stream()
                    .mapToDouble(w -> w.getMasteryScore() != null ? w.getMasteryScore() : 50.0)
                    .average()
                    .orElse(0.0);
            currentMastery = (int) Math.round(avgMastery);
        } else if (problemsAttempted > 0) {
            // User attempted problems but has no recurring weaknesses yet
            if (problemsAccepted > 0) {
                currentMastery = (int) Math.round(((double) problemsAccepted / problemsAttempted) * 100);
                strongestConcept = userSubmissions.stream()
                        .filter(s -> s.getResult() == SubmissionResult.ACCEPTED)
                        .findFirst()
                        .map(s -> !s.getProblem().getTopics().isEmpty() ? s.getProblem().getTopics().get(0) : s.getProblem().getTitle())
                        .orElse("—");
            }
        }

        return DashboardSummaryResponse.builder()
                .problemsAttempted(problemsAttempted)
                .problemsAccepted(problemsAccepted)
                .totalMistakes(totalMistakes)
                .recurringMistakes(recurringMistakes)
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
