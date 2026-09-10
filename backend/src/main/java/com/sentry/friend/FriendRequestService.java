package com.sentry.friend;

import com.sentry.friend.dto.FriendRequestResponse;
import com.sentry.friend.model.FriendRequest;

public interface FriendRequestService {
    FriendRequest sendFriendRequest(Long senderId, String receiverUsername);
    FriendRequest acceptFriendRequest(Long userId, Long requestId);
    FriendRequest declineFriendRequest(Long userId, Long requestId);
    FriendRequest cancelFriendRequest(Long userId, Long requestId);
    FriendRequestResponse getPendingRequests(Long userId);
}
