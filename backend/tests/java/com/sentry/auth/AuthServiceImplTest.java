package com.sentry.auth;

import com.sentry.user.UserService;
import com.sentry.user.model.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    public void testLogin_Success() {
        User existing = User.builder()
                .username("jose")
                .passwordHash("pwd")
                .build();
        when(userService.getUserByUsername("jose")).thenReturn(Optional.of(existing));

        User loggedIn = authService.login("jose", "pwd");
        assertNotNull(loggedIn);
        assertEquals("jose", loggedIn.getUsername());
    }

    @Test
    public void testLogin_InvalidInputs_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> authService.login("  ", "pwd"));
        assertThrows(IllegalArgumentException.class, () -> authService.login("username", null));
    }

    @Test
    public void testLogin_UserNotFound_ThrowsException() {
        when(userService.getUserByUsername("nonexistent")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.login("nonexistent", "pwd"));
    }

    @Test
    public void testLogin_WrongPassword_ThrowsException() {
        User existing = User.builder()
                .username("jose")
                .passwordHash("pwd")
                .build();
        when(userService.getUserByUsername("jose")).thenReturn(Optional.of(existing));
        assertThrows(IllegalArgumentException.class, () -> authService.login("jose", "wrongpwd"));
    }
}
