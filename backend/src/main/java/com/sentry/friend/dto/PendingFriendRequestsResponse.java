package com.sentry.friend.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingFriendRequestsResponse {
    private List<FriendRequestResponse> incoming;
    private List<FriendRequestResponse> outgoing;
}
