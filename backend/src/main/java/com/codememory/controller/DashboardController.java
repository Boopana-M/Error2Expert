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
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(dashboardService.getSummary(userId));
    }

    @GetMapping("/trends")
    public ResponseEntity<List<TrendResponse>> getTrends(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(dashboardService.getTrends(userId));
    }

    @GetMapping("/profile")
    public ResponseEntity<List<MistakeProfileItemResponse>> getMistakeProfile(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(dashboardService.getMistakeProfile(userId));
    }
}
