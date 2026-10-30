package com.codememory.service.impl;

import com.codememory.config.SecurityUtils;
import com.codememory.dto.MistakeResponse;
import com.codememory.entity.Mistake;
import com.codememory.entity.enums.MistakeCategory;
import com.codememory.repository.MistakeRepository;
import com.codememory.service.MistakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MistakeServiceImpl implements MistakeService {

    private final MistakeRepository mistakeRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<MistakeResponse> getAllMistakes() {
        return getAllMistakes(securityUtils.getAuthenticatedUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MistakeResponse> getAllMistakes(Long userId) {
        Long currentUserId = securityUtils.getAuthenticatedUserId();
        Long targetUserId = (userId != null && userId.equals(currentUserId)) ? userId : currentUserId;

        return mistakeRepository.findByUserId(targetUserId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<String> getMistakeCategories() {
        return Arrays.stream(MistakeCategory.values())
                .map(Enum::name)
                .toList();
    }

    private MistakeResponse mapToResponse(Mistake mistake) {
        return MistakeResponse.builder()
                .id(mistake.getId())
                .category(mistake.getCategory())
                .userExplanation(mistake.getUserExplanation())
                .aiExplanation(mistake.getAiExplanation())
                .confidence(mistake.getConfidence())
                .createdAt(mistake.getCreatedAt())
                .build();
    }
}
