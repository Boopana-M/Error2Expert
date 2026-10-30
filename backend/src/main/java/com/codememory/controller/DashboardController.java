package com.codememory.controller;

import com.codememory.dto.DashboardSummaryResponse;
import com.codememory.dto.MistakeProfileItemResponse;
import com.codememory.dto.TrendResponse;
import com.codememory.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(@RequestParam(required = false) Long userId) {
        // Enforce user isolation: always returns authenticated user's summary
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/trends")
    public ResponseEntity<List<TrendResponse>> getTrends(@RequestParam(required = false) Long userId) {
        // Enforce user isolation: always returns authenticated user's trends
        return ResponseEntity.ok(dashboardService.getTrends());
    }

    @GetMapping("/profile")
    public ResponseEntity<List<MistakeProfileItemResponse>> getMistakeProfile(@RequestParam(required = false) Long userId) {
        // Enforce user isolation: always returns authenticated user's mistake profile
        return ResponseEntity.ok(dashboardService.getMistakeProfile());
    }
}
