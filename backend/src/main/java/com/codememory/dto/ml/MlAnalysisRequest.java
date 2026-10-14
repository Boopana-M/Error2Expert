package com.codememory.dto.ml;

import com.codememory.entity.enums.SubmissionResult;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlAnalysisRequest {
    private String problemTitle;
    private List<String> topic;
    private String difficulty;
    private SubmissionResult result;
    private String code;
    private String userExplanation;
}
