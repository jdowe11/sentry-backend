package com.sentry.friend.dto;

import com.sentry.friend.model.FriendRequest;
import com.sentry.user.dto.UserResponse;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRequestResponse {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserResponse sender;
    private UserResponse receiver;

    public static FriendRequestResponse fromFriendRequest(FriendRequest request) {
        if (request == null) {
            return null;
        }
        return FriendRequestResponse.builder()
                .id(request.getId())
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .sender(request.getSender())
                .receiver(request.getReceiver())
                .build();
    }
}
