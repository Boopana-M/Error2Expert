package com.codememory.dto;

import com.codememory.entity.enums.MistakeCategory;
import com.codememory.entity.enums.MistakeStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MistakeResponse {
    private Long id;
    private MistakeCategory category;
    private MistakeStatus status;
    private Long resolvedBySubmissionId;
    private LocalDateTime resolvedAt;
    private String userExplanation;
    private String aiExplanation;
    private Double confidence;
    private LocalDateTime createdAt;
}
