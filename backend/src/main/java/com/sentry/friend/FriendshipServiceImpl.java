package com.sentry.friend;

import com.sentry.friend.model.Friendship;
import com.sentry.user.UserService;
import com.sentry.user.dto.UserResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserService userService;

    @Override
    @Transactional
    public Friendship addFriendship(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("Cannot create a friendship with oneself");
        }

        // Verify users exist
        userService.getUserById(userId1)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId1 + " not found"));
        userService.getUserById(userId2)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId2 + " not found"));

        long u1 = Math.min(userId1, userId2);
        long u2 = Math.max(userId1, userId2);

        Friendship friendship = Friendship.builder()
                .userId1(u1)
                .userId2(u2)
                .build();

        return friendshipRepository.save(friendship);
    }

    @Override
    public List<UserResponse> getFriendsList(Long userId) {
        userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found"));

        return friendshipRepository.findFriendsByUserId(userId);
    }

    @Override
    @Transactional
    public void removeFriendship(Long userId, Long friendId) {
        // Verify users exist
        userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found"));
        userService.getUserById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + friendId + " not found"));

        long u1 = Math.min(userId, friendId);
        long u2 = Math.max(userId, friendId);

        if (!friendshipRepository.exists(u1, u2)) {
            throw new IllegalArgumentException("Friendship does not exist");
        }

        // Delete friendship record
        friendshipRepository.delete(u1, u2);

        // Delete corresponding friend request record
        friendRequestRepository.deleteBySenderIdAndReceiverId(userId, friendId);
    }
}
