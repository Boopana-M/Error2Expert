package com.codememory.controller;

import com.codememory.dto.ApiResponse;
import com.codememory.dto.SubmissionRequest;
import com.codememory.dto.SubmissionResponse;
import com.codememory.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubmissionResponse>> createSubmission(@Valid @RequestBody SubmissionRequest request) {
        SubmissionResponse response = submissionService.createSubmission(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Submission processed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissions(
            @RequestParam(required = false) Long userId) {
        List<SubmissionResponse> responses = (userId != null)
                ? submissionService.getSubmissionsByUserId(userId)
                : submissionService.getAllSubmissions();
        return ResponseEntity.ok(ApiResponse.success("Submissions retrieved successfully", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getSubmissionById(@PathVariable Long id) {
        SubmissionResponse response = submissionService.getSubmissionById(id);
        return ResponseEntity.ok(ApiResponse.success("Submission retrieved successfully", response));
    }
}
