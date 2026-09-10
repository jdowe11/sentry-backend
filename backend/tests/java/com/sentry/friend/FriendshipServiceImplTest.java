package com.sentry.friend;

import com.sentry.friend.model.Friendship;
import com.sentry.user.UserService;
import com.sentry.user.dto.UserResponse;
import com.sentry.user.model.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FriendshipServiceImplTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FriendshipServiceImpl friendshipService;

    @Test
    public void testAddFriendship_Success() {
        User u1 = User.builder().id(1L).username("u1").build();
        User u2 = User.builder().id(2L).username("u2").build();

        when(userService.getUserById(1L)).thenReturn(Optional.of(u1));
        when(userService.getUserById(2L)).thenReturn(Optional.of(u2));

        Friendship f = Friendship.builder().userId1(1L).userId2(2L).build();
        when(friendshipRepository.save(any(Friendship.class))).thenReturn(f);

        Friendship result = friendshipService.addFriendship(1L, 2L);
        assertNotNull(result);
        assertEquals(1L, result.getUserId1());
        assertEquals(2L, result.getUserId2());

        verify(friendshipRepository, times(1)).save(any(Friendship.class));
    }

    @Test
    public void testAddFriendship_SelfFriendship_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> friendshipService.addFriendship(1L, 1L));
    }

    @Test
    public void testAddFriendship_UserNotFound_ThrowsException() {
        when(userService.getUserById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> friendshipService.addFriendship(1L, 2L));
    }

    @Test
    public void testGetFriendsList_Success() {
        User u1 = User.builder().id(1L).username("u1").build();
        UserResponse friend = UserResponse.builder().id(2L).username("friend").build();

        when(userService.getUserById(1L)).thenReturn(Optional.of(u1));
        when(friendshipRepository.findFriendsByUserId(1L)).thenReturn(Arrays.asList(friend));

        List<UserResponse> list = friendshipService.getFriendsList(1L);
        assertEquals(1, list.size());
        assertEquals("friend", list.get(0).getUsername());
    }

    @Test
    public void testRemoveFriendship_Success() {
        User u1 = User.builder().id(1L).username("u1").build();
        User u2 = User.builder().id(2L).username("u2").build();

        when(userService.getUserById(1L)).thenReturn(Optional.of(u1));
        when(userService.getUserById(2L)).thenReturn(Optional.of(u2));
        when(friendshipRepository.exists(1L, 2L)).thenReturn(true);

        doNothing().when(friendshipRepository).delete(1L, 2L);
        doNothing().when(friendRequestRepository).deleteBySenderIdAndReceiverId(1L, 2L);

        assertDoesNotThrow(() -> friendshipService.removeFriendship(1L, 2L));

        verify(friendshipRepository, times(1)).delete(1L, 2L);
        verify(friendRequestRepository, times(1)).deleteBySenderIdAndReceiverId(1L, 2L);
    }

    @Test
    public void testRemoveFriendship_NotFriends_ThrowsException() {
        User u1 = User.builder().id(1L).username("u1").build();
        User u2 = User.builder().id(2L).username("u2").build();

        when(userService.getUserById(1L)).thenReturn(Optional.of(u1));
        when(userService.getUserById(2L)).thenReturn(Optional.of(u2));
        when(friendshipRepository.exists(1L, 2L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> friendshipService.removeFriendship(1L, 2L));
    }
}
