package com.sentry.friend;

import com.sentry.friend.dto.FriendRequestResponse;
import com.sentry.friend.dto.PendingFriendRequestsResponse;

public interface FriendRequestService {
    FriendRequestResponse sendFriendRequest(Long senderId, String receiverUsername);
    FriendRequestResponse acceptFriendRequest(Long userId, Long requestId);
    FriendRequestResponse declineFriendRequest(Long userId, Long requestId);
    FriendRequestResponse cancelFriendRequest(Long userId, Long requestId);
    PendingFriendRequestsResponse getPendingRequests(Long userId);
}
