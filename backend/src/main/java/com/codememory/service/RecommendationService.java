package com.codememory.service;

import com.codememory.dto.RecommendationResponse;
import com.codememory.dto.TodaysFocusResponse;
import com.codememory.entity.Problem;
import com.codememory.entity.User;
import com.codememory.entity.Weakness;
import com.codememory.entity.enums.RecommendationStatus;

import java.util.List;

public interface RecommendationService {
    List<RecommendationResponse> getRecommendationsForCurrentUser();
    List<RecommendationResponse> getRecommendationsForUser(Long userId);
    TodaysFocusResponse getTodaysFocusForCurrentUser();
    TodaysFocusResponse getTodaysFocus(Long userId);
    void generateRecommendationsForWeakness(Weakness weakness);
    void processSolvedProblemFeedback(User user, Problem problem);
    RecommendationResponse updateRecommendationStatus(Long recommendationId, RecommendationStatus status);
}
