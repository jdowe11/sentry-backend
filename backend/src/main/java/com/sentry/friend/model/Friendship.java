package com.sentry.friend.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {
    private Long userId1;
    private Long userId2;
    private LocalDateTime createdAt;
}
