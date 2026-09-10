package com.sentry.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sentry.auth.dto.LoginRequest;
import com.sentry.user.dto.UserResponse;
import com.sentry.user.model.User;

@RestController
@RequestMapping("/api/v1.0")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /// User Authentication (Login)
    @PostMapping("/auth/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest loginRequest) {
        User user = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    /// User Logout
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}
