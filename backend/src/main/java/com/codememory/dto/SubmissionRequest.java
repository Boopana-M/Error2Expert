package com.codememory.dto;

import com.codememory.entity.enums.SubmissionResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionRequest {

    private Long userId;

    private String leetcodeId;

    @NotBlank(message = "Problem title is required")
    private String problemTitle;

    private String difficulty;

    private List<String> topics;

    private String problemUrl;

    private String code;

    private String language;

    @NotNull(message = "Submission result is required")
    private SubmissionResult result;

    private Integer attemptNumber;

    private String userExplanation;
}
