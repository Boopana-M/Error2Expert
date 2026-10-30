package com.codememory.service.impl;

import com.codememory.dto.auth.UserResponse;
import com.codememory.entity.Submission;
import com.codememory.entity.User;
import com.codememory.exception.ResourceNotFoundException;
import com.codememory.repository.*;
import com.codememory.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final MistakeRepository mistakeRepository;
    private final WeaknessRepository weaknessRepository;
    private final RecommendationRepository recommendationRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResourceNotFoundException("Missing email parameter for user lookup.");
        }

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public void resetUserData(Long userId) {
        log.info("Resetting dashboard data for user ID: {}", userId);
        recommendationRepository.deleteAll(recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId));
        weaknessRepository.deleteAll(weaknessRepository.findByUserIdOrderByFrequencyDesc(userId));
        List<Submission> userSubs = submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        for (Submission sub : userSubs) {
            mistakeRepository.deleteAll(mistakeRepository.findBySubmissionId(sub.getId()));
        }
        submissionRepository.deleteAll(userSubs);
        log.info("User ID {} dashboard data successfully wiped clean.", userId);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
