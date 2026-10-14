package com.codememory.controller;

import com.codememory.dto.RecommendationResponse;
import com.codememory.dto.TodaysFocusResponse;
import com.codememory.entity.enums.RecommendationStatus;
import com.codememory.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(recommendationService.getRecommendationsForUser(userId));
    }

    @GetMapping("/today")
    public ResponseEntity<TodaysFocusResponse> getTodaysFocus(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(recommendationService.getTodaysFocus(userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<RecommendationResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam RecommendationStatus status) {
        return ResponseEntity.ok(recommendationService.updateRecommendationStatus(id, status));
    }
}
