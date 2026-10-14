package com.codememory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendResponse {
    private String period;
    private long boundary;
    private long complexity;
    private long edgeCase;
    private long total;
}
