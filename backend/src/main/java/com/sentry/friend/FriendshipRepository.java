package com.sentry.friend;

import com.sentry.friend.model.Friendship;
import com.sentry.user.dto.UserResponse;

import java.util.List;

interface FriendshipRepository {
    Friendship save(Friendship friendship);
    List<UserResponse> findFriendsByUserId(Long userId);
    boolean exists(Long userId1, Long userId2);
    void delete(Long userId1, Long userId2);
}
