package com.sentry.friend;

import com.sentry.friend.dto.FriendRequestResponse;
import com.sentry.friend.dto.PendingFriendRequestsResponse;
import com.sentry.friend.model.FriendRequest;
import com.sentry.user.UserService;
import com.sentry.user.dto.UserResponse;
import com.sentry.user.model.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FriendRequestServiceImplTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserService userService;

    @Mock
    private FriendshipService friendshipService;

    @InjectMocks
    private FriendRequestServiceImpl friendRequestService;

    @Test
    public void testSendFriendRequest_Success() {
        User receiver = User.builder().id(2L).username("bob").build();

        when(userService.getUserByUsername("bob")).thenReturn(Optional.of(receiver));
        when(friendRequestRepository.findBySenderAndReceiver(1L, 2L)).thenReturn(Optional.empty());

        FriendRequest savedRequest = FriendRequest.builder()
                .id(100L)
                .senderId(1L)
                .receiverId(2L)
                .status("pending")
                .build();
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(savedRequest);

        FriendRequestResponse result = friendRequestService.sendFriendRequest(1L, "bob");
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("pending", result.getStatus());
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));
    }

    @Test
    public void testSendFriendRequest_ReceiverNotFound_ThrowsException() {
        when(userService.getUserByUsername("nonexistent")).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            friendRequestService.sendFriendRequest(1L, "nonexistent");
        });
        assertEquals("User with username 'nonexistent' not found", ex.getMessage());
    }

    @Test
    public void testSendFriendRequest_ToSelf_ThrowsException() {
        User alice = User.builder().id(1L).username("alice").build();
        when(userService.getUserByUsername("alice")).thenReturn(Optional.of(alice));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            friendRequestService.sendFriendRequest(1L, "alice");
        });
        assertEquals("You cannot send a friend request to yourself", ex.getMessage());
    }

    @Test
    public void testSendFriendRequest_AlreadyFriends_ThrowsException() {
        User receiver = User.builder().id(2L).username("bob").build();
        FriendRequest existing = FriendRequest.builder().senderId(1L).receiverId(2L).status("accepted").build();

        when(userService.getUserByUsername("bob")).thenReturn(Optional.of(receiver));
        when(friendRequestRepository.findBySenderAndReceiver(1L, 2L)).thenReturn(Optional.of(existing));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            friendRequestService.sendFriendRequest(1L, "bob");
        });
        assertEquals("You are already friends with this user", ex.getMessage());
    }

    @Test
    public void testSendFriendRequest_ActiveFriends_ThrowsException() {
        User receiver = User.builder().id(2L).username("bob").build();

        when(userService.getUserByUsername("bob")).thenReturn(Optional.of(receiver));
        when(friendshipService.getFriendsList(1L)).thenReturn(Arrays.asList(UserResponse.fromUser(receiver)));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            friendRequestService.sendFriendRequest(1L, "bob");
        });
        assertEquals("You are already friends with this user", ex.getMessage());
    }

    @Test
    public void testSendFriendRequest_AlreadySentPending_ThrowsException() {
        User receiver = User.builder().id(2L).username("bob").build();
        FriendRequest existing = FriendRequest.builder().senderId(1L).receiverId(2L).status("pending").build();

        when(userService.getUserByUsername("bob")).thenReturn(Optional.of(receiver));
        when(friendRequestRepository.findBySenderAndReceiver(1L, 2L)).thenReturn(Optional.of(existing));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            friendRequestService.sendFriendRequest(1L, "bob");
        });
        assertEquals("Friend request already sent", ex.getMessage());
    }

    @Test
    public void testSendFriendRequest_IncomingPendingExists_ThrowsException() {
        User receiver = User.builder().id(2L).username("bob").build();
        // Bob sent it to Alice (receiverId = 1, senderId = 2)
        FriendRequest existing = FriendRequest.builder().senderId(2L).receiverId(1L).status("pending").build();

        when(userService.getUserByUsername("bob")).thenReturn(Optional.of(receiver));
        when(friendRequestRepository.findBySenderAndReceiver(1L, 2L)).thenReturn(Optional.of(existing));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            friendRequestService.sendFriendRequest(1L, "bob");
        });
        assertEquals("You already have an incoming friend request from this user", ex.getMessage());
    }

    @Test
    public void testSendFriendRequest_ReactivateDeclined() {
        User receiver = User.builder().id(2L).username("bob").build();
        FriendRequest existing = FriendRequest.builder().id(100L).senderId(2L).receiverId(1L).status("declined").build();

        when(userService.getUserByUsername("bob")).thenReturn(Optional.of(receiver));
        when(friendRequestRepository.findBySenderAndReceiver(1L, 2L)).thenReturn(Optional.of(existing));
        when(friendRequestRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        FriendRequestResponse result = friendRequestService.sendFriendRequest(1L, "bob");
        assertEquals("pending", result.getStatus());
        assertEquals(1L, result.getSenderId());
        assertEquals(2L, result.getReceiverId());
    }

    @Test
    public void testAcceptFriendRequest_Success() {
        FriendRequest request = FriendRequest.builder().id(100L).senderId(2L).receiverId(1L).status("pending").build();
        when(friendRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(request)).thenAnswer(inv -> inv.getArgument(0));
        when(friendshipService.addFriendship(2L, 1L)).thenReturn(null);

        FriendRequestResponse result = friendRequestService.acceptFriendRequest(1L, 100L);
        assertEquals("accepted", result.getStatus());
        verify(friendshipService, times(1)).addFriendship(2L, 1L);
    }

    @Test
    public void testAcceptFriendRequest_Unauthorized_ThrowsException() {
        FriendRequest request = FriendRequest.builder().id(100L).senderId(2L).receiverId(1L).status("pending").build();
        when(friendRequestRepository.findById(100L)).thenReturn(Optional.of(request));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            friendRequestService.acceptFriendRequest(3L, 100L); // User 3 is trying to accept
        });
        assertEquals("Only the receiver can accept a friend request", ex.getMessage());
    }

    @Test
    public void testAcceptFriendRequest_NotPending_ThrowsException() {
        FriendRequest request = FriendRequest.builder().id(100L).senderId(2L).receiverId(1L).status("accepted").build();
        when(friendRequestRepository.findById(100L)).thenReturn(Optional.of(request));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            friendRequestService.acceptFriendRequest(1L, 100L);
        });
        assertEquals("Friend request is not pending", ex.getMessage());
    }

    @Test
    public void testDeclineFriendRequest_Success() {
        FriendRequest request = FriendRequest.builder().id(100L).senderId(2L).receiverId(1L).status("pending").build();
        when(friendRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(request)).thenAnswer(inv -> inv.getArgument(0));

        FriendRequestResponse result = friendRequestService.declineFriendRequest(1L, 100L);
        assertEquals("declined", result.getStatus());
    }

    @Test
    public void testCancelFriendRequest_Success() {
        FriendRequest request = FriendRequest.builder().id(100L).senderId(1L).receiverId(2L).status("pending").build();
        when(friendRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(request)).thenAnswer(inv -> inv.getArgument(0));

        FriendRequestResponse result = friendRequestService.cancelFriendRequest(1L, 100L);
        assertEquals("cancelled", result.getStatus());
    }

    @Test
    public void testGetPendingRequests() {
        FriendRequest r1 = FriendRequest.builder().senderId(1L).receiverId(2L).status("pending").build(); // outgoing
        FriendRequest r2 = FriendRequest.builder().senderId(3L).receiverId(1L).status("pending").build(); // incoming
        when(friendRequestRepository.findPendingByUserId(1L)).thenReturn(Arrays.asList(r1, r2));

        PendingFriendRequestsResponse response = friendRequestService.getPendingRequests(1L);
        assertEquals(1, response.getIncoming().size());
        assertEquals(1, response.getOutgoing().size());
        assertEquals(3L, response.getIncoming().get(0).getSenderId());
        assertEquals(2L, response.getOutgoing().get(0).getReceiverId());
    }
}
