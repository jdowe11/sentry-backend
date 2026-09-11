package com.sentry.friend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentry.friend.dto.FriendRequestResponse;
import com.sentry.friend.dto.PendingFriendRequestsResponse;
import com.sentry.friend.model.FriendRequest;
import com.sentry.user.UserService;
import com.sentry.user.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class FriendRequestServiceImpl implements FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserService userService;
    private final FriendshipService friendshipService;

    @Override
    public FriendRequestResponse sendFriendRequest(Long senderId, String receiverUsername) {
        User receiver = userService.getUserByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("User with username '" + receiverUsername + "' not found"));

        if (senderId.equals(receiver.getId())) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself");
        }
        
        /// Integrity check for Friend List in case their friend request was removed
        boolean areAlreadyFriends = friendshipService.getFriendsList(senderId).stream()
                .anyMatch(u -> u.getId().equals(receiver.getId()));
        if (areAlreadyFriends) {
            throw new IllegalArgumentException("You are already friends with this user");
        }

        Optional<FriendRequest> existingRequestOpt = friendRequestRepository.findBySenderAndReceiver(senderId, receiver.getId());

        if (existingRequestOpt.isPresent()) {
            FriendRequest request = existingRequestOpt.get();
            switch (request.getStatus()) {
                case "accepted":
                    throw new IllegalArgumentException("You are already friends with this user");
                case "pending":
                    if (request.getSenderId().equals(senderId)) {
                        throw new IllegalArgumentException("Friend request already sent");
                    } else {
                        throw new IllegalArgumentException("You already have an incoming friend request from this user");
                    }
                case "declined":
                case "cancelled":
                    // Reactivate the request in the correct direction
                    request.setSenderId(senderId);
                    request.setReceiverId(receiver.getId());
                    request.setStatus("pending");
                    return FriendRequestResponse.fromFriendRequest(friendRequestRepository.save(request));
                default:
                    throw new IllegalStateException("Unexpected friend request status: " + request.getStatus());
            }
        }

        FriendRequest newRequest = FriendRequest.builder()
                .senderId(senderId)
                .receiverId(receiver.getId())
                .status("pending")
                .build();

        return FriendRequestResponse.fromFriendRequest(friendRequestRepository.save(newRequest));
    }

    @Override
    @Transactional
    public FriendRequestResponse acceptFriendRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!"pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        if (!request.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("Only the receiver can accept a friend request");
        }

        request.setStatus("accepted");
        FriendRequest saved = friendRequestRepository.save(request);
        friendshipService.addFriendship(request.getSenderId(), request.getReceiverId());
        return FriendRequestResponse.fromFriendRequest(saved);
    }

    @Override
    public FriendRequestResponse declineFriendRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!"pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        if (!request.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("Only the receiver can decline a friend request");
        }

        request.setStatus("declined");
        return FriendRequestResponse.fromFriendRequest(friendRequestRepository.save(request));
    }

    @Override
    public FriendRequestResponse cancelFriendRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!"pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        if (!request.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("Only the sender can cancel a friend request");
        }

        request.setStatus("cancelled");
        return FriendRequestResponse.fromFriendRequest(friendRequestRepository.save(request));
    }

    @Override
    public PendingFriendRequestsResponse getPendingRequests(Long userId) {
        List<FriendRequest> pending = friendRequestRepository.findPendingByUserId(userId);
        List<FriendRequestResponse> incoming = new ArrayList<>();
        List<FriendRequestResponse> outgoing = new ArrayList<>();

        for (FriendRequest request : pending) {
            if (request.getReceiverId().equals(userId)) {
                incoming.add(FriendRequestResponse.fromFriendRequest(request));
            } else if (request.getSenderId().equals(userId)) {
                outgoing.add(FriendRequestResponse.fromFriendRequest(request));
            }
        }

        return PendingFriendRequestsResponse.builder()
                .incoming(incoming)
                .outgoing(outgoing)
                .build();
    }
}
