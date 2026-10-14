package com.codememory.dto;

import com.codememory.entity.enums.SubmissionResult;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long problemId;
    private String problemTitle;
    private String difficulty;
    private List<String> topics;
    private SubmissionResult result;
    private Integer attemptNumber;
    private String code;
    private String language;
    private LocalDateTime submittedAt;
    private MistakeResponse mistake;
}
