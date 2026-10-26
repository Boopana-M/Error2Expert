package com.codememory.controller;

import com.codememory.dto.ApiResponse;
import com.codememory.dto.auth.UserResponse;
import com.codememory.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        String email = (authentication != null) ? authentication.getName() : null;
        UserResponse response = userService.getCurrentUser(email);
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", response));
    }
}
