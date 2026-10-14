package com.codememory.service.impl;

import com.codememory.dto.WeaknessResponse;
import com.codememory.entity.Mistake;
import com.codememory.entity.Weakness;
import com.codememory.exception.ResourceNotFoundException;
import com.codememory.repository.MistakeRepository;
import com.codememory.repository.SubmissionRepository;
import com.codememory.repository.WeaknessRepository;
import com.codememory.service.WeaknessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WeaknessServiceImpl implements WeaknessService {

    private final WeaknessRepository weaknessRepository;
    private final MistakeRepository mistakeRepository;
    private final SubmissionRepository submissionRepository;
    private final com.codememory.repository.UserRepository userRepository;

    public WeaknessServiceImpl(
            WeaknessRepository weaknessRepository,
            MistakeRepository mistakeRepository,
            SubmissionRepository submissionRepository,
            com.codememory.repository.UserRepository userRepository) {
        this.weaknessRepository = weaknessRepository;
        this.mistakeRepository = mistakeRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeaknessResponse> getWeaknessesForUser(Long userId) {
        Long targetUserId = (userId != null) ? userId : resolveDefaultUserId();
        return weaknessRepository.findByUserIdOrderByFrequencyDesc(targetUserId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Long resolveDefaultUserId() {
        return userRepository.findAll().stream().findFirst().map(u -> u.getId()).orElse(1L);
    }

    @Override
    @Transactional(readOnly = true)
    public WeaknessResponse getWeaknessById(Long id) {
        Weakness weakness = weaknessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weakness not found with ID: " + id));
        return mapToResponse(weakness);
    }

    private WeaknessResponse mapToResponse(Weakness w) {
        // Derive severity
        String severity = "MEDIUM";
        double score = w.getMasteryScore() != null ? w.getMasteryScore() : 70.0;
        if (score >= 80.0) {
            severity = "IMPROVING";
        } else if (w.getFrequency() >= 3) {
            severity = "HIGH";
        }

        // Find affected problem titles
        List<String> affectedProblems = mistakeRepository.findAll().stream()
                .filter(m -> m.getCategory() == w.getMistakeCategory() &&
                        m.getSubmission().getUser().getId().equals(w.getUser().getId()))
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
