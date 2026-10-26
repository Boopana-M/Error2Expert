package com.codememory.service.impl;

import com.codememory.dto.auth.UserResponse;
import com.codememory.entity.User;
import com.codememory.exception.ResourceNotFoundException;
import com.codememory.repository.UserRepository;
import com.codememory.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = (email != null && !email.isBlank())
                ? userRepository.findByEmail(email.toLowerCase())
                    .orElseGet(this::getFallbackDefaultUser)
                : getFallbackDefaultUser();

        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return mapToResponse(user);
    }

    private User getFallbackDefaultUser() {
        return userRepository.findAll().stream().findFirst().orElseThrow(
                () -> new ResourceNotFoundException("No user profile initialized in database."));
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
