package com.codememory.service;

import com.codememory.dto.auth.AuthResponse;
import com.codememory.dto.auth.LoginRequest;
import com.codememory.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
