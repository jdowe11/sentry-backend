package com.sentry.friend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentry.friend.dto.FriendRequestResponse;
import com.sentry.friend.dto.SendFriendRequest;
import com.sentry.friend.dto.UpdateStatusRequest;
import com.sentry.friend.model.FriendRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;

@WebMvcTest(FriendRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.sentry.config.WebConfig.class)
public class FriendRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendRequestService friendRequestService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSendFriendRequest_Success() throws Exception {
        SendFriendRequest payload = SendFriendRequest.builder().receiverUsername("bob").build();
        FriendRequest created = FriendRequest.builder().id(10L).senderId(1L).receiverId(2L).status("pending").build();

        when(friendRequestService.sendFriendRequest(anyLong(), anyString())).thenReturn(created);

        mockMvc.perform(post("/api/v1.0/friend-requests")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    public void testSendFriendRequest_BadRequest() throws Exception {
        SendFriendRequest payload = SendFriendRequest.builder().receiverUsername("bob").build();

        when(friendRequestService.sendFriendRequest(anyLong(), anyString()))
                .thenThrow(new IllegalArgumentException("You cannot send a friend request to yourself"));

        mockMvc.perform(post("/api/v1.0/friend-requests")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot send a friend request to yourself"));
    }

    @Test
    public void testUpdateFriendRequestStatus_Accepted() throws Exception {
        UpdateStatusRequest payload = UpdateStatusRequest.builder().status("accepted").build();
        FriendRequest updated = FriendRequest.builder().id(10L).senderId(2L).receiverId(1L).status("accepted").build();

        when(friendRequestService.acceptFriendRequest(1L, 10L)).thenReturn(updated);

        mockMvc.perform(patch("/api/v1.0/friend-requests/10/status")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));
    }

    @Test
    public void testUpdateFriendRequestStatus_Declined() throws Exception {
        UpdateStatusRequest payload = UpdateStatusRequest.builder().status("declined").build();
        FriendRequest updated = FriendRequest.builder().id(10L).senderId(2L).receiverId(1L).status("declined").build();

        when(friendRequestService.declineFriendRequest(1L, 10L)).thenReturn(updated);

        mockMvc.perform(patch("/api/v1.0/friend-requests/10/status")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("declined"));
    }

    @Test
    public void testUpdateFriendRequestStatus_Cancelled() throws Exception {
        UpdateStatusRequest payload = UpdateStatusRequest.builder().status("cancelled").build();
        FriendRequest updated = FriendRequest.builder().id(10L).senderId(1L).receiverId(2L).status("cancelled").build();

        when(friendRequestService.cancelFriendRequest(1L, 10L)).thenReturn(updated);

        mockMvc.perform(patch("/api/v1.0/friend-requests/10/status")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));
    }

    @Test
    public void testGetPendingRequests_Success() throws Exception {
        FriendRequest incoming = FriendRequest.builder().id(5L).senderId(2L).receiverId(1L).status("pending").build();
        FriendRequest outgoing = FriendRequest.builder().id(6L).senderId(1L).receiverId(3L).status("pending").build();

        FriendRequestResponse response = FriendRequestResponse.builder()
                .incoming(Collections.singletonList(incoming))
                .outgoing(Collections.singletonList(outgoing))
                .build();

        when(friendRequestService.getPendingRequests(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1.0/friend-requests/pending")
                .header("Authorization", "Bearer 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incoming[0].id").value(5))
                .andExpect(jsonPath("$.outgoing[0].id").value(6));
    }

    @Test
    public void testSendFriendRequest_BlankReceiverUsername() throws Exception {
        SendFriendRequest payload = SendFriendRequest.builder().receiverUsername("  ").build();

        mockMvc.perform(post("/api/v1.0/friend-requests")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSendFriendRequest_UsernameTooShort() throws Exception {
        SendFriendRequest payload = SendFriendRequest.builder().receiverUsername("ab").build();

        mockMvc.perform(post("/api/v1.0/friend-requests")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateFriendRequestStatus_InvalidStatus() throws Exception {
        UpdateStatusRequest payload = UpdateStatusRequest.builder().status("invalid_status_here").build();

        mockMvc.perform(patch("/api/v1.0/friend-requests/10/status")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateFriendRequestStatus_BlankStatus() throws Exception {
        UpdateStatusRequest payload = UpdateStatusRequest.builder().status("   ").build();

        mockMvc.perform(patch("/api/v1.0/friend-requests/10/status")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateFriendRequestStatus_IllegalStateException() throws Exception {
        UpdateStatusRequest payload = UpdateStatusRequest.builder().status("accepted").build();

        when(friendRequestService.acceptFriendRequest(anyLong(), anyLong()))
                .thenThrow(new IllegalStateException("Unexpected status state"));

        mockMvc.perform(patch("/api/v1.0/friend-requests/10/status")
                .header("Authorization", "Bearer 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unexpected status state"));
    }
}
