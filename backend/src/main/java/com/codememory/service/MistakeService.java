package com.codememory.service;

import com.codememory.dto.MistakeResponse;

import java.util.List;

public interface MistakeService {
    List<MistakeResponse> getAllMistakes(Long userId);
    List<String> getMistakeCategories();
}
