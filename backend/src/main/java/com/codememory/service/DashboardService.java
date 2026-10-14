package com.codememory.service;

import com.codememory.dto.DashboardSummaryResponse;
import com.codememory.dto.MistakeProfileItemResponse;
import com.codememory.dto.TrendResponse;

import java.util.List;

public interface DashboardService {
    DashboardSummaryResponse getSummary(Long userId);
    List<TrendResponse> getTrends(Long userId);
    List<MistakeProfileItemResponse> getMistakeProfile(Long userId);
}
