package com.codememory.service;

import com.codememory.dto.auth.UserResponse;

public interface UserService {
    UserResponse getCurrentUser(String email);
    UserResponse getUserById(Long id);
}
