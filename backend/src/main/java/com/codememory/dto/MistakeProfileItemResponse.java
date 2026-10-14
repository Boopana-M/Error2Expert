package com.codememory.dto;

import com.codememory.entity.enums.MistakeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MistakeProfileItemResponse {
    private MistakeCategory category;
    private String label;
    private long count;
    private int percentage;
    private String color;
}
