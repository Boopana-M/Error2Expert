package com.codememory.controller;

import com.codememory.dto.MistakeProfileItemResponse;
import com.codememory.dto.MistakeResponse;
import com.codememory.service.DashboardService;
import com.codememory.service.MistakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mistakes")
@CrossOrigin(origins = "*")
public class MistakeController {

    private final MistakeService mistakeService;
    private final DashboardService dashboardService;

    public MistakeController(MistakeService mistakeService, DashboardService dashboardService) {
        this.mistakeService = mistakeService;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<List<MistakeResponse>> getAllMistakes(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(mistakeService.getAllMistakes(userId));
    }

    @GetMapping("/profile")
    public ResponseEntity<List<MistakeProfileItemResponse>> getMistakeProfile(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(dashboardService.getMistakeProfile(userId));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(mistakeService.getMistakeCategories());
    }
}
