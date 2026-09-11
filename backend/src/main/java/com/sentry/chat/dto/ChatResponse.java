package com.sentry.chat.dto;

import com.sentry.user.dto.UserResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private Long id;
    private LocalDateTime createdAt;
    private List<UserResponse> participants;
    private MessageResponse lastMessage;
}
