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
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(@RequestParam(required = false) Long userId) {
        // Enforce user isolation: always returns authenticated user's recommendations
        return ResponseEntity.ok(recommendationService.getRecommendationsForCurrentUser());
    }

    @GetMapping("/today")
    public ResponseEntity<TodaysFocusResponse> getTodaysFocus(@RequestParam(required = false) Long userId) {
        // Enforce user isolation: always returns authenticated user's focus
        return ResponseEntity.ok(recommendationService.getTodaysFocusForCurrentUser());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<RecommendationResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam RecommendationStatus status) {
        return ResponseEntity.ok(recommendationService.updateRecommendationStatus(id, status));
    }
}
