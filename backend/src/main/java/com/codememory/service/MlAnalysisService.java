package com.codememory.service;

import com.codememory.dto.ml.MlAnalysisRequest;
import com.codememory.dto.ml.MlAnalysisResponse;

public interface MlAnalysisService {
    MlAnalysisResponse analyzeAttempt(MlAnalysisRequest request);
}
