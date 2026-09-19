package com.sentry.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentry.chat.dto.ChatResponse;
import com.sentry.chat.dto.CreateDirectChatRequest;
import com.sentry.chat.dto.MessageResponse;
import com.sentry.chat.dto.SendMessageRequest;
import com.sentry.user.dto.UserResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.sentry.config.WebConfig.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetOrCreateDirectChat_Success() throws Exception {
        CreateDirectChatRequest request = CreateDirectChatRequest.builder().recipientId(2L).build();
        UserResponse user1 = UserResponse.builder().id(1L).username("alice").displayName("Alice").build();
        UserResponse user2 = UserResponse.builder().id(2L).username("bob").displayName("Bob").build();
        ChatResponse response = ChatResponse.builder()
                .id(100L)
                .createdAt(LocalDateTime.now())
                .participants(List.of(user1, user2))
                .build();

        when(chatService.getOrCreateDirectChat(1L, 2L)).thenReturn(response);

        mockMvc.perform(post("/api/v1.0/chats/direct")
                        .header("Authorization", "Bearer 1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.participants.length()").value(2));
    }

    @Test
    public void testGetOrCreateDirectChat_InvalidPayload_BadRequest() throws Exception {
        CreateDirectChatRequest request = CreateDirectChatRequest.builder().recipientId(null).build();

        mockMvc.perform(post("/api/v1.0/chats/direct")
                        .header("Authorization", "Bearer 1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetUserChats_Success() throws Exception {
        ChatResponse chat1 = ChatResponse.builder().id(1L).build();
        ChatResponse chat2 = ChatResponse.builder().id(2L).build();

        when(chatService.getUserChats(1L)).thenReturn(List.of(chat1, chat2));

        mockMvc.perform(get("/api/v1.0/chats")
                        .header("Authorization", "Bearer 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    public void testGetChatById_Success() throws Exception {
        ChatResponse chat = ChatResponse.builder().id(5L).build();
        when(chatService.getChatById(1L, 5L)).thenReturn(chat);

        mockMvc.perform(get("/api/v1.0/chats/5")
                        .header("Authorization", "Bearer 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    public void testSendMessage_Success() throws Exception {
        String validCiphertext = "dGhpcyBpcyBhIHZhbGlkIGJhc2U2NCBlbmNyeXB0ZWQgY2lwaGVydGV4dCBleGFtcGxl";
        SendMessageRequest request = SendMessageRequest.builder().ciphertext(validCiphertext).build();
        MessageResponse response = MessageResponse.builder()
                .id(10L)
                .chatId(1L)
                .senderId(1L)
                .ciphertext(validCiphertext)
                .createdAt(LocalDateTime.now())
                .build();

        when(chatService.sendMessage(1L, 1L, validCiphertext)).thenReturn(response);

        mockMvc.perform(post("/api/v1.0/chats/1/messages")
                        .header("Authorization", "Bearer 1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.ciphertext").value(validCiphertext));
    }

    @Test
    public void testSendMessage_BlankCiphertext_BadRequest() throws Exception {
        SendMessageRequest request = SendMessageRequest.builder().ciphertext("   ").build();

        mockMvc.perform(post("/api/v1.0/chats/1/messages")
                        .header("Authorization", "Bearer 1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetChatMessages_Success() throws Exception {
        MessageResponse m1 = MessageResponse.builder().id(1L).ciphertext("msg1").build();
        MessageResponse m2 = MessageResponse.builder().id(2L).ciphertext("msg2").build();

        when(chatService.getChatMessages(1L, 1L, 50, null)).thenReturn(List.of(m1, m2));

        mockMvc.perform(get("/api/v1.0/chats/1/messages")
                        .header("Authorization", "Bearer 1")
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ciphertext").value("msg1"))
                .andExpect(jsonPath("$[1].ciphertext").value("msg2"));
    }

    @Test
    public void testLeaveChat_Success() throws Exception {
        doNothing().when(chatService).leaveChat(1L, 1L);

        mockMvc.perform(delete("/api/v1.0/chats/1/participants/me")
                        .header("Authorization", "Bearer 1"))
                .andExpect(status().isOk());

        verify(chatService, times(1)).leaveChat(1L, 1L);
    }
}
