package com.sentry.user.dto;

import lombok.*;

import java.time.LocalDateTime;

import com.sentry.user.model.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String displayName;
    private LocalDateTime createdAt;

    public static UserResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
