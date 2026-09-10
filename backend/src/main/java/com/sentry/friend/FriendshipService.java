package com.sentry.friend;

import com.sentry.friend.model.Friendship;
import com.sentry.user.dto.UserResponse;

import java.util.List;

public interface FriendshipService {
    Friendship addFriendship(Long userId1, Long userId2);
    List<UserResponse> getFriendsList(Long userId);
    void removeFriendship(Long userId, Long friendId);
}
