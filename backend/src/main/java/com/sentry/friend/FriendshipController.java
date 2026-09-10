package com.sentry.friend;

import com.sentry.common.annotation.CurrentUserId;
import com.sentry.user.dto.UserResponse;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1.0")
@Validated
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @GetMapping("/friends")
    public ResponseEntity<List<UserResponse>> getFriendsList(@Parameter(hidden = true) @CurrentUserId Long userId) {
        List<UserResponse> friends = friendshipService.getFriendsList(userId);
        return ResponseEntity.ok(friends);
    }

    @DeleteMapping("/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Min(1) Long friendId) {
        friendshipService.removeFriendship(userId, friendId);
        return ResponseEntity.ok().build();
    }
}
