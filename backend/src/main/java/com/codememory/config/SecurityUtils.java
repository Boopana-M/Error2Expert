package com.codememory.config;

import com.codememory.entity.User;
import com.codememory.exception.UnauthorizedException;
import com.codememory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated. Please log in.");
        }

        String email = auth.getName();
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Missing authenticated principal.");
        }

        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found for email: " + email));
    }

    public Long getAuthenticatedUserId() {
        return getAuthenticatedUser().getId();
    }
}
