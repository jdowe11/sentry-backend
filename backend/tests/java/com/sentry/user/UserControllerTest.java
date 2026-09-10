package com.sentry.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentry.user.dto.CreateUserRequest;
import com.sentry.user.dto.UpdateDisplayNameRequest;
import com.sentry.user.dto.UpdateUsernameRequest;
import com.sentry.user.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.Import;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.sentry.config.WebConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==========================================
    // createUser Endpoint Tests
    // ==========================================

    @Test
    public void testCreateUser_Success() throws Exception {
        User savedUser = User.builder()
                .id(5L)
                .username("newguy")
                .displayName("New Guy")
                .passwordHash("hashed")
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(savedUser);

        String rawJson = "{\"username\":\"newguy\",\"displayName\":\"New Guy\",\"passwordHash\":\"hashed\"}";

        mockMvc.perform(post("/api/v1.0/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("newguy"));
    }

    @Test
    public void testCreateUser_Failure_BadRequest() throws Exception {
        CreateUserRequest inputUser = CreateUserRequest.builder().username("").displayName("").build();

        mockMvc.perform(post("/api/v1.0/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputUser)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // getUserById Endpoint Tests
    // ==========================================

    @Test
    public void testGetUserById_Found() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("gamer123")
                .displayName("CoolGamer")
                .passwordHash("hash")
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1.0/users/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("gamer123"))
                .andExpect(jsonPath("$.displayName").value("CoolGamer"));
    }

    @Test
    public void testGetUserById_NotFound() throws Exception {
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1.0/users/99")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // getUserByUsername Endpoint Tests
    // ==========================================

    @Test
    public void testGetUserByUsername_Found() throws Exception {
        User user = User.builder()
                .id(2L)
                .username("findme")
                .displayName("Find Me")
                .build();

        when(userService.getUserByUsername("findme")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1.0/users/username/findme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("findme"));
    }

    @Test
    public void testGetUserByUsername_NotFound() throws Exception {
        when(userService.getUserByUsername("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1.0/users/username/missing"))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // getAllUsers Endpoint Tests
    // ==========================================

    @Test
    public void testGetAllUsers_Success() throws Exception {
        User u1 = User.builder().id(10L).username("user10").build();
        User u2 = User.builder().id(11L).username("user11").build();

        when(userService.getAllUsers()).thenReturn(Arrays.asList(u1, u2));

        mockMvc.perform(get("/api/v1.0/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("user10"))
                .andExpect(jsonPath("$[1].username").value("user11"));
    }

    // ==========================================
    // deleteUser Endpoint Tests
    // ==========================================

    @Test
    public void testDeleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/v1.0/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteUser_Failure_BadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Invalid ID")).when(userService).deleteUser(-1L);

        mockMvc.perform(delete("/api/v1.0/users/-1"))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // updateUsername Endpoint Tests
    // ==========================================

    @Test
    public void testUpdateUsername_Success() throws Exception {
        UpdateUsernameRequest payload = UpdateUsernameRequest.builder().newUsername("alice-new").build();
        User updated = User.builder().id(1L).username("alice-new").displayName("Alice").build();

        when(userService.updateUsername(eq(1L), any(UpdateUsernameRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1.0/users/me/username")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice-new"));
    }

    @Test
    public void testUpdateUsername_Failure() throws Exception {
        UpdateUsernameRequest payload = UpdateUsernameRequest.builder().newUsername("alice space").build();

        mockMvc.perform(patch("/api/v1.0/users/me/username")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // updateDisplayName Endpoint Tests
    // ==========================================

    @Test
    public void testUpdateDisplayName_Success() throws Exception {
        UpdateDisplayNameRequest payload = UpdateDisplayNameRequest.builder().newDisplayName("Alice Updated").build();
        User updated = User.builder().id(1L).username("alice").displayName("Alice Updated").build();

        when(userService.updateDisplayName(eq(1L), any(UpdateDisplayNameRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1.0/users/me/display-name")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice Updated"));
    }

    @Test
    public void testUpdateDisplayName_Failure() throws Exception {
        UpdateDisplayNameRequest payload = UpdateDisplayNameRequest.builder().newDisplayName("").build();

        mockMvc.perform(patch("/api/v1.0/users/me/display-name")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // searchUsers Endpoint Tests
    // ==========================================

    @Test
    public void testSearchUsers_Success() throws Exception {
        User u1 = User.builder().id(10L).username("user10").displayName("User Ten").build();
        when(userService.searchUsers("ten")).thenReturn(List.of(u1));

        mockMvc.perform(get("/api/v1.0/users/search").param("q", "ten"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("user10"))
                .andExpect(jsonPath("$[0].displayName").value("User Ten"));
    }

    @Test
    public void testSearchUsers_EmptyQuery() throws Exception {
        mockMvc.perform(get("/api/v1.0/users/search").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testCreateUser_UsernameTooLong() throws Exception {
        String json = "{\"username\":\"thisusernameislongerthanthirtytwocharslong\",\"displayName\":\"Good Name\",\"passwordHash\":\"pwd\"}";
        mockMvc.perform(post("/api/v1.0/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateUser_UsernameInvalidCharacters() throws Exception {
        String json = "{\"username\":\"invalid user!\",\"displayName\":\"Good Name\",\"passwordHash\":\"pwd\"}";
        mockMvc.perform(post("/api/v1.0/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateUser_DisplayNameBlank() throws Exception {
        String json = "{\"username\":\"goodusername\",\"displayName\":\"   \",\"passwordHash\":\"pwd\"}";
        mockMvc.perform(post("/api/v1.0/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateUser_DisplayNameTooLong() throws Exception {
        String json = "{\"username\":\"goodusername\",\"displayName\":\"thisdisplaynameislongerthanfiftycharacterslongtobedureaboutlimit\",\"passwordHash\":\"pwd\"}";
        mockMvc.perform(post("/api/v1.0/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }
}
