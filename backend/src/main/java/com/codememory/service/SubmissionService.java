package com.codememory.service;

import com.codememory.dto.SubmissionRequest;
import com.codememory.dto.SubmissionResponse;

import java.util.List;

public interface SubmissionService {
    SubmissionResponse createSubmission(SubmissionRequest request);
    SubmissionResponse getSubmissionById(Long id);
    List<SubmissionResponse> getSubmissionsByUserId(Long userId);
    List<SubmissionResponse> getAllSubmissions();
}
