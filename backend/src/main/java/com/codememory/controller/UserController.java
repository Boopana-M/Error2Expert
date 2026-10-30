package com.codememory.controller;

import com.codememory.config.SecurityUtils;
import com.codememory.dto.ApiResponse;
import com.codememory.dto.auth.UserResponse;
import com.codememory.entity.User;
import com.codememory.exception.UnauthorizedException;
import com.codememory.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        User user = securityUtils.getAuthenticatedUser();
        UserResponse response = userService.getCurrentUser(user.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = securityUtils.getAuthenticatedUser();
        if (!user.getId().equals(id)) {
            throw new UnauthorizedException("Cannot access another user's profile.");
        }
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", response));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<String>> resetMyDashboard() {
        User user = securityUtils.getAuthenticatedUser();
        userService.resetUserData(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Dashboard successfully reset to completely empty state.", "RESET_SUCCESS"));
    }
}
