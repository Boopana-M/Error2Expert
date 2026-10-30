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
@RequiredArgsConstructor
public class MistakeController {

    private final MistakeService mistakeService;
    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<List<MistakeResponse>> getAllMistakes(@RequestParam(required = false) Long userId) {
        // Enforce user isolation: always returns authenticated user's mistakes
        return ResponseEntity.ok(mistakeService.getAllMistakes());
    }

    @GetMapping("/profile")
    public ResponseEntity<List<MistakeProfileItemResponse>> getMistakeProfile(@RequestParam(required = false) Long userId) {
        // Enforce user isolation: always returns authenticated user's profile
        return ResponseEntity.ok(dashboardService.getMistakeProfile());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(mistakeService.getMistakeCategories());
    }
}
