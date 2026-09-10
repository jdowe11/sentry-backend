package com.sentry.user;

import com.sentry.common.annotation.CurrentUserId;
import com.sentry.user.dto.CreateUserRequest;
import com.sentry.user.dto.UpdateDisplayNameRequest;
import com.sentry.user.dto.UpdateUsernameRequest;
import com.sentry.user.dto.UserResponse;
import com.sentry.user.model.User;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1.0")
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /// User Registration
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User created = userService.createUser(request);
        return ResponseEntity.ok(UserResponse.fromUser(created));
    }

    /// Retrieve currently authenticated user profile
    @GetMapping("/users/me")
    public ResponseEntity<UserResponse> getMe(@Parameter(hidden = true) @CurrentUserId Long userId) {
        return userService.getUserById(userId)
                .map(UserResponse::fromUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /// Update currently authenticated user's username
    @PatchMapping("/users/me/username")
    public ResponseEntity<UserResponse> updateUsername(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Valid @RequestBody UpdateUsernameRequest request
    ) {
        User updated = userService.updateUsername(userId, request);
        return ResponseEntity.ok(UserResponse.fromUser(updated));
    }

    /// Update currently authenticated user's display name
    @PatchMapping("/users/me/display-name")
    public ResponseEntity<UserResponse> updateDisplayName(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Valid @RequestBody UpdateDisplayNameRequest request
    ) {
        User updated = userService.updateDisplayName(userId, request);
        return ResponseEntity.ok(UserResponse.fromUser(updated));
    }

    /// Retrieve a user by id
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(UserResponse::fromUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /// Retrieve a user by username
    @GetMapping("/users/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(UserResponse::fromUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /// Retrieve all users
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::fromUser)
                .toList();
        return ResponseEntity.ok(users);
    }

    /// Delete a user by id
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /// Search users by query
    @GetMapping("/users/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<UserResponse> users = userService.searchUsers(query.trim()).stream()
                .map(UserResponse::fromUser)
                .toList();
        return ResponseEntity.ok(users);
    }
}
