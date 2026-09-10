package com.sentry.friend;

import java.util.List;
import java.util.Optional;

import com.sentry.friend.model.FriendRequest;

interface FriendRequestRepository {
    FriendRequest save(FriendRequest request);
    Optional<FriendRequest> findById(Long id);
    Optional<FriendRequest> findBySenderAndReceiver(Long senderId, Long receiverId);
    List<FriendRequest> findPendingByUserId(Long userId);
    void deleteById(Long id);
    /// Never called on friendrequest endpoints, is a job for deleting friends
    void deleteBySenderIdAndReceiverId(Long senderId, Long receiverId);
}
