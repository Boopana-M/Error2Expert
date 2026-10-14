package com.codememory.service;

import com.codememory.dto.WeaknessResponse;

import java.util.List;

public interface WeaknessService {
    List<WeaknessResponse> getWeaknessesForUser(Long userId);
    WeaknessResponse getWeaknessById(Long id);
}
