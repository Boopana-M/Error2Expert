package com.codememory.service.impl;

import com.codememory.dto.ml.MlAnalysisRequest;
import com.codememory.dto.ml.MlAnalysisResponse;
import com.codememory.entity.enums.MistakeCategory;
import com.codememory.entity.enums.SubmissionResult;
import com.codememory.service.MlAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MlAnalysisServiceImpl implements MlAnalysisService {

    private final RestClient restClient;
    private final String mlServiceUrl;

    public MlAnalysisServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${ml.service.url:http://localhost:8000}") String mlServiceUrl) {
        String targetUrl = mlServiceUrl != null ? mlServiceUrl.trim() : "http://localhost:8000";
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            targetUrl = "http://" + targetUrl;
        }
        this.mlServiceUrl = targetUrl;
        this.restClient = restClientBuilder
                .baseUrl(targetUrl)
                .build();
    }

    @Override
    public MlAnalysisResponse analyzeAttempt(MlAnalysisRequest request) {
        try {
            log.info("Sending attempt analysis request to Python ML service at {}", mlServiceUrl);

            MlAnalysisResponse response = restClient.post()
                    .uri("/analyze-attempt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MlAnalysisResponse.class);

            if (response != null && response.getMistakeCategory() != null) {
                log.info("Received analysis from ML service: category={}, confidence={}",
                        response.getMistakeCategory(), response.getConfidence());
                return response;
            }
        } catch (RestClientException e) {
            log.warn("Python ML Service unavailable at {} ({}: {}). Falling back to rule-based engine.",
                    mlServiceUrl, e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error contacting ML service: {}", e.getMessage(), e);
        }

        return fallbackAnalysis(request);
    }

    private MlAnalysisResponse fallbackAnalysis(MlAnalysisRequest request) {
        String explanation = (request.getUserExplanation() != null) ? request.getUserExplanation().toLowerCase() : "";
        List<String> topics = (request.getTopic() != null)
                ? request.getTopic().stream().map(String::toLowerCase).toList()
                : List.of();
        SubmissionResult result = request.getResult();

        MistakeCategory category;
        String rootCause;
        List<String> evidence = new ArrayList<>();

        if (result == SubmissionResult.COMPILE_ERROR || explanation.contains("syntax") || explanation.contains("compile")) {
            category = MistakeCategory.SYNTAX_OR_API_ERROR;
            rootCause = "Language syntax or API usage error prevented execution.";
            evidence.add("Submission outcome: COMPILE_ERROR");
        } else if (result == SubmissionResult.TIME_LIMIT_EXCEEDED || explanation.contains("tle") || explanation.contains("nested") || explanation.contains("complexity")) {
            category = MistakeCategory.COMPLEXITY_ERROR;
            rootCause = "Algorithmic complexity misconception exceeding problem time constraints.";
            evidence.add("Submission outcome: TIME_LIMIT_EXCEEDED");
        } else if (explanation.contains("off by one") || explanation.contains("<= instead of <") || explanation.contains("< instead of <=")) {
            category = MistakeCategory.OFF_BY_ONE_ERROR;
            rootCause = "Off-by-one indexing offset in loop termination condition.";
            evidence.add("User explanation reported indexing offset");
        } else if (explanation.contains("visit") || explanation.contains("cycle") || topics.contains("graph") || topics.contains("depth-first search")) {
            category = MistakeCategory.GRAPH_VISITED_ERROR;
            rootCause = "Omitted or improperly maintained visited-state transition causing cyclic infinite traversal.";
            evidence.add("Graph visited-state cues identified");
        } else if (explanation.contains("bound") || explanation.contains("high = mid") || explanation.contains("low = mid") || explanation.contains("pointer") || topics.contains("binary search")) {
            category = MistakeCategory.BOUNDARY_ERROR;
            rootCause = "Incorrect boundary update or loop termination in pointer logic on " + request.getProblemTitle() + ".";
            evidence.add("Boundary cues identified in binary search / pointer logic");
        } else if (explanation.contains("null") || explanation.contains("empty") || explanation.contains("edge")) {
            category = MistakeCategory.EDGE_CASE_ERROR;
            rootCause = "Failure to guard against edge-case input values.";
            evidence.add("Edge case / null handling cues detected");
        } else if (explanation.contains("base case") || explanation.contains("recursion")) {
            category = MistakeCategory.RECURSION_BASE_CASE_ERROR;
            rootCause = "Missing or prematurely triggered base case in recursive decomposition.";
            evidence.add("Recursion base case cues detected");
        } else if (explanation.contains("dp") || explanation.contains("state") || topics.contains("dynamic programming")) {
            category = MistakeCategory.DP_STATE_ERROR;
            rootCause = "Misformulated state representation or incorrect transition equation.";
            evidence.add("Dynamic programming state cues detected");
        } else {
            category = MistakeCategory.WRONG_LOGIC;
            rootCause = "Algorithmic decision logic did not satisfy expected assertions on " + request.getProblemTitle() + ".";
            evidence.add("General logic failure fallback");
        }

        double confidence = (request.getUserExplanation() != null && !request.getUserExplanation().isBlank()) ? 0.90 : 0.75;

        return MlAnalysisResponse.builder()
                .mistakeCategory(category)
                .confidence(confidence)
                .rootCause(rootCause)
                .similarMistakes(0)
                .recurring(false)
                .evidence(evidence)
                .build();
    }
}
