package com.codememory.controller;

import com.codememory.dto.WeaknessResponse;
import com.codememory.service.WeaknessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weaknesses")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WeaknessController {

    private final WeaknessService weaknessService;

    @GetMapping
    public ResponseEntity<List<WeaknessResponse>> getWeaknesses(@RequestParam(required = false) Long userId) {
        // Enforce user isolation: always returns authenticated user's weaknesses
        return ResponseEntity.ok(weaknessService.getWeaknessesForCurrentUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeaknessResponse> getWeaknessById(@PathVariable Long id) {
        return ResponseEntity.ok(weaknessService.getWeaknessById(id));
    }
}
