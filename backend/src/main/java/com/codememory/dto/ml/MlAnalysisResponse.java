package com.codememory.dto.ml;

import com.codememory.entity.enums.MistakeCategory;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlAnalysisResponse {
    private MistakeCategory mistakeCategory;
    private Double confidence;
    private String rootCause;
    private Integer similarMistakes;
    private Boolean recurring;
    private List<String> evidence;
}
