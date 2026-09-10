package com.sentry.auth;

import com.sentry.user.UserService;
import com.sentry.user.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class AuthServiceImpl implements AuthService {

    private final UserService userService;

    @Override
    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password cannot be blank");
        }
        User user = userService.getUserByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        // Temporary plain text comparison (will update when password hashing is introduced)
        if (!user.getPasswordHash().equals(password)) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        return user;
    }
}
