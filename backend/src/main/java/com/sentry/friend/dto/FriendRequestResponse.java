package com.sentry.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import com.sentry.friend.model.FriendRequest;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponse {
    private List<FriendRequest> incoming;
    private List<FriendRequest> outgoing;
}
