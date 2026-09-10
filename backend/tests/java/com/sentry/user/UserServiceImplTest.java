package com.sentry.user;

import com.sentry.user.dto.UpdateDisplayNameRequest;
import com.sentry.user.dto.UpdateUsernameRequest;
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
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void testCreateUser_Success() {
        User user = User.builder()
                .username("testuser")
                .displayName("Test User")
                .passwordHash("hashedpwd")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User created = userService.createUser(user);
        assertNotNull(created);
        assertEquals("testuser", created.getUsername());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testCreateUser_UsernameExists_ThrowsException() {
        User user = User.builder()
                .username("existing")
                .displayName("Test User")
                .passwordHash("hashedpwd")
                .build();

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(user);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testGetUserById_Success() {
        User user = User.builder().id(1L).username("testuser").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> found = userService.getUserById(1L);
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    public void testGetUserById_InvalidId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(null));
        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(0L));
    }

    @Test
    public void testGetUserByUsername_Success() {
        User user = User.builder().id(1L).username("testuser").build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Optional<User> found = userService.getUserByUsername("testuser");
        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getId());
    }

    @Test
    public void testGetAllUsers_Success() {
        User u1 = User.builder().id(1L).username("user1").build();
        User u2 = User.builder().id(2L).username("user2").build();
        when(userRepository.findAll()).thenReturn(Arrays.asList(u1, u2));

        List<User> list = userService.getAllUsers();
        assertEquals(2, list.size());
        assertEquals("user1", list.get(0).getUsername());
    }

    @Test
    public void testDeleteUser_Success() {
        doNothing().when(userRepository).deleteById(1L);
        assertDoesNotThrow(() -> userService.deleteUser(1L));
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testUpdateUsername_Success() {
        User existing = User.builder()
                .id(1L)
                .username("jose")
                .displayName("Jose GOAT")
                .passwordHash("pwd")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUsernameRequest request = UpdateUsernameRequest.builder()
                .newUsername("jose-new")
                .build();

        User updated = userService.updateUsername(1L, request);
        assertEquals("jose-new", updated.getUsername());
        assertEquals("Jose GOAT", updated.getDisplayName());
    }

    @Test
    public void testUpdateUsername_UserNotFound_ThrowsException() {
        UpdateUsernameRequest request = UpdateUsernameRequest.builder().newUsername("username").build();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.updateUsername(999L, request));
    }

    @Test
    public void testUpdateUsername_UsernameAlreadyTaken_ThrowsException() {
        User existing = User.builder().id(1L).username("jose").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        UpdateUsernameRequest request = UpdateUsernameRequest.builder().newUsername("taken").build();
        assertThrows(IllegalArgumentException.class, () -> userService.updateUsername(1L, request));
    }

    @Test
    public void testUpdateDisplayName_Success() {
        User existing = User.builder()
                .id(1L)
                .username("jose")
                .displayName("Jose GOAT")
                .passwordHash("pwd")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateDisplayNameRequest request = UpdateDisplayNameRequest.builder()
                .newDisplayName("Jose updated")
                .build();

        User updated = userService.updateDisplayName(1L, request);
        assertEquals("Jose updated", updated.getDisplayName());
        assertEquals("jose", updated.getUsername());
    }

    @Test
    public void testUpdateDisplayName_UserNotFound_ThrowsException() {
        UpdateDisplayNameRequest request = UpdateDisplayNameRequest.builder().newDisplayName("name").build();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.updateDisplayName(999L, request));
    }

    @Test
    public void testSearchUsers_Success() {
        User alice = User.builder().id(1L).username("alice").displayName("Alice").build();
        when(userRepository.searchByUsername("alice")).thenReturn(Arrays.asList(alice));

        List<User> results = userService.searchUsers("alice");
        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).getUsername());
        verify(userRepository, times(1)).searchByUsername("alice");
    }

    @Test
    public void testSearchUsers_NullQuery_ReturnsEmptyList() {
        List<User> results = userService.searchUsers(null);
        assertTrue(results.isEmpty());
        verify(userRepository, never()).searchByUsername(anyString());
    }

    @Test
    public void testSearchUsers_BlankQuery_ReturnsEmptyList() {
        List<User> results = userService.searchUsers("   ");
        assertTrue(results.isEmpty());
        verify(userRepository, never()).searchByUsername(anyString());
    }
}
