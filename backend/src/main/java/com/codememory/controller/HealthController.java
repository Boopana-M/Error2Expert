package com.codememory.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public health check controller for cloud infrastructure (Render, Docker, Kubernetes).
 * Returns HTTP 200 without requiring JWT authentication.
 */
@RestController
public class HealthController {

    @GetMapping({"/health", "/api/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "error2expert-backend",
                "timestamp", Instant.now().toString()
        ));
    }
}
