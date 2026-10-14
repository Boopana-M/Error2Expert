package com.codememory.service.impl;

import com.codememory.dto.MistakeResponse;
import com.codememory.dto.SubmissionRequest;
import com.codememory.dto.SubmissionResponse;
import com.codememory.entity.*;
import com.codememory.entity.enums.MistakeCategory;
import com.codememory.entity.enums.SubmissionResult;
import com.codememory.exception.ResourceNotFoundException;
import com.codememory.repository.*;
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
    private final UserRepository userRepository;
    private final MistakeRepository mistakeRepository;
    private final WeaknessRepository weaknessRepository;

    @Override
    @Transactional
    public SubmissionResponse createSubmission(SubmissionRequest request) {
        log.info("Processing submission for problem '{}', result: {}", request.getProblemTitle(), request.getResult());

        // 1. Resolve User (provided or default demo user)
        User user = resolveUser(request.getUserId());

        // 2. Resolve Problem (lookup or auto-register)
        Problem problem = resolveProblem(request);

        // 3. Calculate Attempt Number
        int attemptNumber = (request.getAttemptNumber() != null && request.getAttemptNumber() > 0)
                ? request.getAttemptNumber()
                : (int) submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(user.getId(), problem.getId()).size() + 1;

        // 4. Save Submission
        Submission submission = Submission.builder()
                .user(user)
                .problem(problem)
                .code(request.getCode())
                .language(request.getLanguage() != null ? request.getLanguage().toLowerCase() : "java")
                .result(request.getResult())
                .attemptNumber(attemptNumber)
                .build();

        Submission savedSubmission = submissionRepository.save(submission);

        // 5. Mistake Analysis & Longitudinal Weakness Tracking (if failed or explanation provided)
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

        Mistake mistake = mistakeRepository.findBySubmissionId(id).stream().findFirst().orElse(null);
        return mapToResponse(submission, mistake);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByUserId(Long userId) {
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId)
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
        return submissionRepository.findAll()
                .stream()
                .map(sub -> {
                    Mistake mistake = mistakeRepository.findBySubmissionId(sub.getId()).stream().findFirst().orElse(null);
                    return mapToResponse(sub, mistake);
                })
                .toList();
    }

    private User resolveUser(Long userId) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseGet(this::getOrCreateDefaultUser);
        }
        return getOrCreateDefaultUser();
    }

    private User getOrCreateDefaultUser() {
        return userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User demo = User.builder()
                    .name("Boopana")
                    .email("boopana@example.com")
                    .passwordHash("$2a$10$7vN3Vp0lP7kUvT8p7D9Qe.5eL61yZ3xK8aP8tJzV9e1uR4eK0j7fG")
                    .build();
            return userRepository.save(demo);
        });
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

        // Create new problem
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
        MistakeCategory category = classifyMistakeBaseline(submission.getResult(), userExplanation, problem.getTopics());

        String aiExplanation = generateBaselineExplanation(category, problem.getTitle());
        double confidence = (userExplanation != null && !userExplanation.isBlank()) ? 0.92 : 0.78;

        Mistake mistake = Mistake.builder()
                .submission(submission)
                .category(category)
                .userExplanation(userExplanation)
                .aiExplanation(aiExplanation)
                .confidence(confidence)
                .build();

        Mistake savedMistake = mistakeRepository.save(mistake);

        // Update Longitudinal Weakness Model
        updateWeaknessProfile(submission.getUser(), problem, category);

        return savedMistake;
    }

    private MistakeCategory classifyMistakeBaseline(SubmissionResult result, String explanation, List<String> topics) {
        String exp = (explanation != null) ? explanation.toLowerCase() : "";
        String topicStr = (topics != null) ? String.join(" ", topics).toLowerCase() : "";

        if (exp.contains("bound") || exp.contains("high = mid") || exp.contains("low = mid") || exp.contains("pointer") || exp.contains("index")) {
            return MistakeCategory.BOUNDARY_ERROR;
        }
        if (exp.contains("off by one") || exp.contains("<= instead of <") || exp.contains("< instead of <=")) {
            return MistakeCategory.OFF_BY_ONE_ERROR;
        }
        if (exp.contains("visit") || exp.contains("cycle") || topicStr.contains("graph")) {
            return MistakeCategory.GRAPH_VISITED_ERROR;
        }
        if (result == SubmissionResult.TIME_LIMIT_EXCEEDED || exp.contains("tle") || exp.contains("nested") || exp.contains("complexity") || exp.contains("slow")) {
            return MistakeCategory.COMPLEXITY_ERROR;
        }
        if (exp.contains("null") || exp.contains("empty") || exp.contains("corner") || exp.contains("edge")) {
            return MistakeCategory.EDGE_CASE_ERROR;
        }
        if (exp.contains("base case") || exp.contains("recursion") || exp.contains("stack overflow")) {
            return MistakeCategory.RECURSION_BASE_CASE_ERROR;
        }
        if (exp.contains("dp") || exp.contains("state") || exp.contains("memo")) {
            return MistakeCategory.DP_STATE_ERROR;
        }
        if (result == SubmissionResult.COMPILE_ERROR || exp.contains("syntax") || exp.contains("type")) {
            return MistakeCategory.SYNTAX_OR_API_ERROR;
        }
        return MistakeCategory.WRONG_LOGIC;
    }

    private String generateBaselineExplanation(MistakeCategory category, String problemTitle) {
        return switch (category) {
            case BOUNDARY_ERROR -> "Detected boundary handling miscalculation while updating pointers on " + problemTitle + ".";
            case OFF_BY_ONE_ERROR -> "Identified off-by-one indexing offset in loop termination condition.";
            case GRAPH_VISITED_ERROR -> "Missing or improperly maintained visited-state transition during traversal.";
            case COMPLEXITY_ERROR -> "Sub-optimal algorithmic complexity exceeded problem time constraints.";
            case EDGE_CASE_ERROR -> "Failed to properly guard against edge case inputs (e.g. empty, single element, or extreme values).";
            case RECURSION_BASE_CASE_ERROR -> "Missing or improperly ordered recursion termination base-case.";
            case DP_STATE_ERROR -> "Sub-problem state definition or recurrence transition relationship misformulation.";
            default -> "Algorithmic logic failure identified on " + problemTitle + ".";
        };
    }

    private void updateWeaknessProfile(User user, Problem problem, MistakeCategory category) {
        String concept = (!problem.getTopics().isEmpty()) ? problem.getTopics().get(0) : problem.getTitle();

        Optional<Weakness> existingWeakness = weaknessRepository.findByUserIdAndConceptAndMistakeCategory(
                user.getId(), concept, category);

        LocalDateTime now = LocalDateTime.now();
        if (existingWeakness.isPresent()) {
            Weakness weakness = existingWeakness.get();
            weakness.setFrequency(weakness.getFrequency() + 1);
            weakness.setLastObserved(now);
            // Decay mastery score upon repeated mistake
            double currentMastery = (weakness.getMasteryScore() != null) ? weakness.getMasteryScore() : 100.0;
            weakness.setMasteryScore(Math.max(10.0, currentMastery - 5.0));
            weakness.setConfidence(Math.min(0.98, (weakness.getConfidence() != null ? weakness.getConfidence() : 0.8) + 0.05));
            weaknessRepository.save(weakness);
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
            weaknessRepository.save(newWeakness);
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
