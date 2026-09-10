package com.sentry.friend;

import com.sentry.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.Import;

@WebMvcTest(FriendshipController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.sentry.config.WebConfig.class)
public class FriendshipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendshipService friendshipService;

    @Test
    public void testGetFriendsList_Success() throws Exception {
        UserResponse friend = UserResponse.builder().id(2L).username("friend").displayName("Friend User").build();
        when(friendshipService.getFriendsList(1L)).thenReturn(Arrays.asList(friend));

        mockMvc.perform(get("/api/v1.0/friends")
                        .header("Authorization", "Bearer 1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].username").value("friend"));
    }

    @Test
    public void testRemoveFriend_Success() throws Exception {
        doNothing().when(friendshipService).removeFriendship(1L, 2L);

        mockMvc.perform(delete("/api/v1.0/friends/2")
                        .header("Authorization", "Bearer 1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(friendshipService, times(1)).removeFriendship(1L, 2L);
    }

    @Test
    public void testRemoveFriend_InvalidId_BadRequest() throws Exception {
        mockMvc.perform(delete("/api/v1.0/friends/0")
                        .header("Authorization", "Bearer 1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
