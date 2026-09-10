package com.sentry.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentry.auth.dto.LoginRequest;
import com.sentry.user.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==========================================
    // POST /auth/login Tests
    // ==========================================

    @Test
    public void testLogin_Success() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("jose")
                .displayName("Jose GOAT")
                .build();

        when(authService.login("jose", "pwd")).thenReturn(user);

        LoginRequest request = LoginRequest.builder()
                .username("jose")
                .password("pwd")
                .build();

        mockMvc.perform(post("/api/v1.0/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jose"))
                .andExpect(jsonPath("$.displayName").value("Jose GOAT"));
    }

    @Test
    public void testLogin_Failure_InvalidCredentials() throws Exception {
        when(authService.login("jose", "wrongpwd"))
                .thenThrow(new IllegalArgumentException("Invalid username or password"));

        LoginRequest request = LoginRequest.builder()
                .username("jose")
                .password("wrongpwd")
                .build();

        mockMvc.perform(post("/api/v1.0/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // POST /auth/logout Tests
    // ==========================================

    @Test
    public void testLogout_Success() throws Exception {
        mockMvc.perform(post("/api/v1.0/auth/logout"))
                .andExpect(status().isOk());
    }
}
