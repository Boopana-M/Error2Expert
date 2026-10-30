package com.codememory.service.impl;

import com.codememory.config.SecurityUtils;
import com.codememory.dto.MistakeResponse;
import com.codememory.dto.SubmissionRequest;
import com.codememory.dto.SubmissionResponse;
import com.codememory.dto.ml.MlAnalysisRequest;
import com.codememory.dto.ml.MlAnalysisResponse;
import com.codememory.entity.Mistake;
import com.codememory.entity.Problem;
import com.codememory.entity.Submission;
import com.codememory.entity.User;
import com.codememory.entity.Weakness;
import com.codememory.entity.enums.MistakeCategory;
import com.codememory.entity.enums.SubmissionResult;
import com.codememory.exception.ResourceNotFoundException;
import com.codememory.repository.MistakeRepository;
import com.codememory.repository.ProblemRepository;
import com.codememory.repository.SubmissionRepository;
import com.codememory.repository.WeaknessRepository;
import com.codememory.service.MlAnalysisService;
import com.codememory.service.RecommendationService;
import com.codememory.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final MistakeRepository mistakeRepository;
    private final WeaknessRepository weaknessRepository;
    private final MlAnalysisService mlAnalysisService;
    private final RecommendationService recommendationService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public SubmissionResponse createSubmission(SubmissionRequest request) {
        log.info("Processing submission for problem '{}', result: {}", request.getProblemTitle(), request.getResult());

        // 1. Resolve Authenticated User directly from SecurityContext
        User user = securityUtils.getAuthenticatedUser();

        // 2. Resolve Problem (lookup or auto-register in catalog)
        Problem problem = resolveProblem(request);

        // 3. Calculate Attempt Number for this specific user
        int attemptNumber = (request.getAttemptNumber() != null && request.getAttemptNumber() > 0)
                ? request.getAttemptNumber()
                : (int) submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(user.getId(), problem.getId()).size() + 1;

        // 4. Save Submission tied strictly to authenticated user
        Submission submission = Submission.builder()
                .user(user)
                .problem(problem)
                .code(request.getCode())
                .language(request.getLanguage() != null ? request.getLanguage().toLowerCase() : "java")
                .result(request.getResult())
                .attemptNumber(attemptNumber)
                .build();

        Submission savedSubmission = submissionRepository.save(submission);

        // 5. Adaptive Feedback Loop: If accepted, resolve recommendations and boost mastery
        if (savedSubmission.getResult() == SubmissionResult.ACCEPTED) {
            recommendationService.processSolvedProblemFeedback(user, problem);
        }

        // 6. Mistake Analysis & Longitudinal Weakness Tracking (if failed or explanation provided)
        Mistake mistake = null;
        if (savedSubmission.getResult() != SubmissionResult.ACCEPTED || request.getUserExplanation() != null) {
            mistake = recordMistakeAndTrackWeakness(savedSubmission, problem, request.getUserExplanation());
        }

        return mapToResponse(savedSubmission, mistake);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionById(Long id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + id));

        Long currentUserId = securityUtils.getAuthenticatedUserId();
        if (!submission.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Submission not found with ID: " + id);
        }

        Mistake mistake = mistakeRepository.findBySubmissionId(id).stream().findFirst().orElse(null);
        return mapToResponse(submission, mistake);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsForCurrentUser() {
        return getSubmissionsByUserId(securityUtils.getAuthenticatedUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByUserId(Long userId) {
        Long currentUserId = securityUtils.getAuthenticatedUserId();
        // Disallow reading other users' submissions
        Long targetUserId = (userId != null && userId.equals(currentUserId)) ? userId : currentUserId;

        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(targetUserId)
                .stream()
                .map(sub -> {
                    Mistake mistake = mistakeRepository.findBySubmissionId(sub.getId()).stream().findFirst().orElse(null);
                    return mapToResponse(sub, mistake);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getAllSubmissions() {
        // Enforce user data isolation: return only authenticated user's submissions
        return getSubmissionsForCurrentUser();
    }

    private Problem resolveProblem(SubmissionRequest request) {
        Optional<Problem> existing = Optional.empty();
        if (request.getLeetcodeId() != null && !request.getLeetcodeId().isBlank()) {
            existing = problemRepository.findByLeetcodeId(request.getLeetcodeId());
        }
        if (existing.isEmpty() && request.getProblemTitle() != null) {
            existing = problemRepository.findByTitleIgnoreCase(request.getProblemTitle().trim());
        }

        if (existing.isPresent()) {
            Problem p = existing.get();
            if (request.getTopics() != null && !request.getTopics().isEmpty()) {
                for (String topic : request.getTopics()) {
                    if (!p.getTopics().contains(topic)) {
                        p.getTopics().add(topic);
                    }
                }
                problemRepository.save(p);
            }
            return p;
        }

        // Create new problem in shared catalog
        List<String> topics = (request.getTopics() != null) ? new ArrayList<>(request.getTopics()) : new ArrayList<>();
        Problem newProblem = Problem.builder()
                .leetcodeId(request.getLeetcodeId())
                .title(request.getProblemTitle().trim())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : "MEDIUM")
                .url(request.getProblemUrl())
                .topics(topics)
                .build();

        return problemRepository.save(newProblem);
    }

    private Mistake recordMistakeAndTrackWeakness(Submission submission, Problem problem, String userExplanation) {
        MlAnalysisRequest mlRequest = MlAnalysisRequest.builder()
                .userId(String.valueOf(submission.getUser().getId()))
                .problemTitle(problem.getTitle())
                .topic(problem.getTopics())
                .difficulty(problem.getDifficulty())
                .result(submission.getResult())
                .code(submission.getCode())
                .userExplanation(userExplanation)
                .build();

        MlAnalysisResponse mlResponse = mlAnalysisService.analyzeAttempt(mlRequest);

        MistakeCategory category = (mlResponse != null && mlResponse.getMistakeCategory() != null)
                ? mlResponse.getMistakeCategory()
                : MistakeCategory.WRONG_LOGIC;

        String aiExplanation = (mlResponse != null && mlResponse.getRootCause() != null)
                ? mlResponse.getRootCause()
                : "Algorithmic logic failure identified on " + problem.getTitle() + ".";

        double confidence = (mlResponse != null && mlResponse.getConfidence() != null)
                ? mlResponse.getConfidence()
                : 0.85;

        Mistake mistake = Mistake.builder()
                .submission(submission)
                .category(category)
                .userExplanation(userExplanation)
                .aiExplanation(aiExplanation)
                .confidence(confidence)
                .build();

        Mistake savedMistake = mistakeRepository.save(mistake);

        // Update Longitudinal Weakness Model for this user
        updateWeaknessProfile(submission.getUser(), problem, category);

        return savedMistake;
    }

    private void updateWeaknessProfile(User user, Problem problem, MistakeCategory category) {
        String concept = (!problem.getTopics().isEmpty()) ? problem.getTopics().get(0) : problem.getTitle();

        Optional<Weakness> existingWeakness = weaknessRepository.findByUserIdAndConceptAndMistakeCategory(
                user.getId(), concept, category);

        LocalDateTime now = LocalDateTime.now();
        Weakness savedWeakness;
        if (existingWeakness.isPresent()) {
            Weakness weakness = existingWeakness.get();
            weakness.setFrequency(weakness.getFrequency() + 1);
            weakness.setLastObserved(now);
            // Decay mastery score upon repeated mistake
            double currentMastery = (weakness.getMasteryScore() != null) ? weakness.getMasteryScore() : 100.0;
            weakness.setMasteryScore(Math.max(10.0, currentMastery - 5.0));
            weakness.setConfidence(Math.min(0.98, (weakness.getConfidence() != null ? weakness.getConfidence() : 0.8) + 0.05));
            savedWeakness = weaknessRepository.save(weakness);
        } else {
            Weakness newWeakness = Weakness.builder()
                    .user(user)
                    .concept(concept)
                    .mistakeCategory(category)
                    .frequency(1)
                    .confidence(0.85)
                    .masteryScore(95.0)
                    .firstObserved(now)
                    .lastObserved(now)
                    .build();
            savedWeakness = weaknessRepository.save(newWeakness);
        }

        // Trigger targeted recommendation generation if recurring (frequency >= 2)
        if (savedWeakness.getFrequency() >= 2) {
            recommendationService.generateRecommendationsForWeakness(savedWeakness);
        }
    }

    private SubmissionResponse mapToResponse(Submission submission, Mistake mistake) {
        MistakeResponse mistakeResponse = null;
        if (mistake != null) {
            mistakeResponse = MistakeResponse.builder()
                    .id(mistake.getId())
                    .category(mistake.getCategory())
                    .userExplanation(mistake.getUserExplanation())
                    .aiExplanation(mistake.getAiExplanation())
                    .confidence(mistake.getConfidence())
                    .createdAt(mistake.getCreatedAt())
                    .build();
        }

        return SubmissionResponse.builder()
                .id(submission.getId())
                .userId(submission.getUser().getId())
                .userName(submission.getUser().getName())
                .problemId(submission.getProblem().getId())
                .problemTitle(submission.getProblem().getTitle())
                .difficulty(submission.getProblem().getDifficulty())
                .topics(submission.getProblem().getTopics())
                .result(submission.getResult())
                .attemptNumber(submission.getAttemptNumber())
                .code(submission.getCode())
                .language(submission.getLanguage())
                .submittedAt(submission.getSubmittedAt())
                .mistake(mistakeResponse)
                .build();
    }
}
