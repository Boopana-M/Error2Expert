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
        List<String> affectedProblems = mistakeRepository.findByUserIdAndCategory(w.getUser().getId(), w.getMistakeCategory())
                .stream()
                .map(m -> m.getSubmission().getProblem().getTitle())
                .distinct()
                .limit(4)
                .toList();

        if (affectedProblems.isEmpty()) {
            affectedProblems = List.of(w.getConcept() + " Problem");
        }

        String description = String.format("Repeated %s mistake pattern identified across %s exercises.",
                w.getMistakeCategory().name().replace('_', ' ').toLowerCase(),
                w.getConcept());

        return WeaknessResponse.builder()
                .id(w.getId())
                .concept(w.getConcept())
                .mistakeCategory(w.getMistakeCategory())
                .severity(severity)
                .frequency(w.getFrequency())
                .confidence(w.getConfidence() != null ? w.getConfidence() : 0.85)
                .masteryScore(score)
                .firstObserved(w.getFirstObserved())
                .lastObserved(w.getLastObserved())
                .description(description)
                .affectedProblems(affectedProblems)
                .build();
    }
}
