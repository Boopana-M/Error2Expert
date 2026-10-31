package com.codememory.service.impl;

import com.codememory.config.SecurityUtils;
import com.codememory.dto.WeaknessResponse;
import com.codememory.entity.Mistake;
import com.codememory.entity.Weakness;
import com.codememory.exception.ResourceNotFoundException;
import com.codememory.repository.MistakeRepository;
import com.codememory.repository.WeaknessRepository;
import com.codememory.service.WeaknessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeaknessServiceImpl implements WeaknessService {

    private final WeaknessRepository weaknessRepository;
    private final MistakeRepository mistakeRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<WeaknessResponse> getWeaknessesForCurrentUser() {
        return getWeaknessesForUser(securityUtils.getAuthenticatedUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeaknessResponse> getWeaknessesForUser(Long userId) {
        Long currentUserId = securityUtils.getAuthenticatedUserId();
        Long targetUserId = (userId != null && userId.equals(currentUserId)) ? userId : currentUserId;

        return weaknessRepository.findByUserIdOrderByFrequencyDesc(targetUserId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WeaknessResponse getWeaknessById(Long id) {
        Weakness weakness = weaknessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weakness not found with ID: " + id));

        Long currentUserId = securityUtils.getAuthenticatedUserId();
        if (!weakness.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Weakness not found with ID: " + id);
        }

        return mapToResponse(weakness);
    }

    private WeaknessResponse mapToResponse(Weakness w) {
        String severity = "MEDIUM";
        double score = w.getMasteryScore() != null ? w.getMasteryScore() : 70.0;
        if (score >= 80.0) {
            severity = "IMPROVING";
        } else if (w.getFrequency() >= 3) {
            severity = "HIGH";
        }

        // Find affected problem titles strictly for this user and category
        List<Mistake> categoryMistakes = mistakeRepository.findByUserIdAndCategory(w.getUser().getId(), w.getMistakeCategory());

        List<String> affectedProblems = categoryMistakes.stream()
                .map(m -> m.getSubmission().getProblem().getTitle())
                .distinct()
                .limit(4)
                .toList();

        if (affectedProblems.isEmpty()) {
            affectedProblems = List.of(w.getConcept() + " Problem");
        }

        int totalCategoryMistakes = categoryMistakes.size();
        int resolvedCount = (int) categoryMistakes.stream()
                .filter(m -> m.getStatus() == com.codememory.entity.enums.MistakeStatus.RESOLVED)
                .count();
        int openCount = totalCategoryMistakes - resolvedCount;

        int recoveryRate = totalCategoryMistakes > 0
                ? (int) Math.round(((double) resolvedCount / totalCategoryMistakes) * 100)
                : 100;

        String categoryName = w.getMistakeCategory().name().replace('_', ' ').toLowerCase();
        String learningState;
        String learningNarrative;

        if (totalCategoryMistakes == 0 || (recoveryRate >= 70 && score >= 75.0)) {
            learningState = "IMPROVING";
            learningNarrative = String.format("Improving — You previously encountered %s errors in %s, but successfully corrected them in recent attempts (%d%% recovery).",
                    categoryName, w.getConcept(), recoveryRate);
        } else if (score >= 85.0 && openCount == 0) {
            learningState = "MASTERED";
            learningNarrative = String.format("Mastered — High algorithmic consistency across %s with all past failure modes resolved.", w.getConcept());
        } else if (w.getFrequency() >= 3 && recoveryRate < 45) {
            learningState = "RECURRING_WEAKNESS";
            learningNarrative = String.format("Recurring Weakness — %s errors repeat across %s with a %d%% recovery rate. Targeted practice recommended.",
                    categoryName, w.getConcept(), recoveryRate);
        } else {
            learningState = "LEARNING_IN_PROGRESS";
            learningNarrative = String.format("Learning in Progress — Actively resolving %s in %s (%d resolved, %d pending).",
                    categoryName, w.getConcept(), resolvedCount, openCount);
        }

        String description = String.format("Mistake pattern: %s in %s (%d occurrences, %d resolved).",
                categoryName, w.getConcept(), w.getFrequency(), resolvedCount);

        return WeaknessResponse.builder()
                .id(w.getId())
                .concept(w.getConcept())
                .mistakeCategory(w.getMistakeCategory())
                .severity(severity)
                .frequency(w.getFrequency())
                .confidence(w.getConfidence() != null ? w.getConfidence() : 0.85)
                .masteryScore(score)
                .resolvedCount(resolvedCount)
                .openCount(openCount)
                .recoveryRate(recoveryRate)
                .learningState(learningState)
                .learningNarrative(learningNarrative)
                .firstObserved(w.getFirstObserved())
                .lastObserved(w.getLastObserved())
                .description(description)
                .affectedProblems(affectedProblems)
                .build();
    }
}
