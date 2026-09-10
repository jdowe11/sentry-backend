package com.sentry.friend;

import com.sentry.common.annotation.CurrentUserId;
import com.sentry.friend.dto.FriendRequestResponse;
import com.sentry.friend.dto.SendFriendRequest;
import com.sentry.friend.dto.UpdateStatusRequest;
import com.sentry.friend.model.FriendRequest;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1.0")
@Validated
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    @PostMapping("/friend-requests")
    public ResponseEntity<FriendRequest> sendFriendRequest(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Valid @RequestBody SendFriendRequest request
    ) {
        FriendRequest fr = friendRequestService.sendFriendRequest(userId, request.getReceiverUsername());
        return ResponseEntity.ok(fr);
    }

    @PatchMapping("/friend-requests/{id}/status")
    public ResponseEntity<FriendRequest> updateFriendRequestStatus(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        String status = request.getStatus().toLowerCase().trim();

        FriendRequest updated;
        switch (status) {
            case "accepted":
                updated = friendRequestService.acceptFriendRequest(userId, id);
                break;
            case "declined":
                updated = friendRequestService.declineFriendRequest(userId, id);
                break;
            case "cancelled":
                updated = friendRequestService.cancelFriendRequest(userId, id);
                break;
            default:
                throw new IllegalArgumentException("Invalid status transition");
        }
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/friend-requests/pending")
    public ResponseEntity<FriendRequestResponse> getPendingRequests(@Parameter(hidden = true) @CurrentUserId Long userId) {
        FriendRequestResponse response = friendRequestService.getPendingRequests(userId);
        return ResponseEntity.ok(response);
    }
}
