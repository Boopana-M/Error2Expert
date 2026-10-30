package com.codememory.service;

import com.codememory.dto.DashboardSummaryResponse;
import com.codememory.dto.MistakeProfileItemResponse;
import com.codememory.dto.TrendResponse;

import java.util.List;

public interface DashboardService {
    DashboardSummaryResponse getSummary();
    DashboardSummaryResponse getSummary(Long userId);

    List<TrendResponse> getTrends();
    List<TrendResponse> getTrends(Long userId);

    List<MistakeProfileItemResponse> getMistakeProfile();
    List<MistakeProfileItemResponse> getMistakeProfile(Long userId);
}
