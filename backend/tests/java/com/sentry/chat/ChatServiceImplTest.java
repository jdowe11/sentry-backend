package com.sentry.chat;

import com.sentry.chat.dto.ChatResponse;
import com.sentry.chat.dto.MessageResponse;
import com.sentry.chat.model.Chat;
import com.sentry.chat.model.Message;
import com.sentry.user.UserService;
import com.sentry.user.dto.UserResponse;
import com.sentry.user.model.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceImplTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserService userService;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    public void testGetOrCreateDirectChat_ExistingChat_ReturnsExisting() {
        User recipient = User.builder().id(2L).username("bob").build();
        Chat existingChat = Chat.builder().id(10L).createdAt(LocalDateTime.now()).build();
        UserResponse u1 = UserResponse.builder().id(1L).username("alice").build();
        UserResponse u2 = UserResponse.builder().id(2L).username("bob").build();

        when(userService.getUserById(2L)).thenReturn(Optional.of(recipient));
        when(chatRepository.findDirectChat(1L, 2L)).thenReturn(Optional.of(existingChat));
        when(chatRepository.findParticipants(10L)).thenReturn(List.of(u1, u2));
        when(chatRepository.findLatestMessage(10L)).thenReturn(Optional.empty());

        ChatResponse result = chatService.getOrCreateDirectChat(1L, 2L);
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(2, result.getParticipants().size());
        verify(chatRepository, never()).createChat();
    }

    @Test
    public void testGetOrCreateDirectChat_NewChat_CreatesAndAddsParticipants() {
        User recipient = User.builder().id(2L).username("bob").build();
        Chat newChat = Chat.builder().id(20L).createdAt(LocalDateTime.now()).build();
        UserResponse u1 = UserResponse.builder().id(1L).username("alice").build();
        UserResponse u2 = UserResponse.builder().id(2L).username("bob").build();

        when(userService.getUserById(2L)).thenReturn(Optional.of(recipient));
        when(chatRepository.findDirectChat(1L, 2L)).thenReturn(Optional.empty());
        when(chatRepository.createChat()).thenReturn(newChat);
        when(chatRepository.findParticipants(20L)).thenReturn(List.of(u1, u2));
        when(chatRepository.findLatestMessage(20L)).thenReturn(Optional.empty());

        ChatResponse result = chatService.getOrCreateDirectChat(1L, 2L);
        assertNotNull(result);
        assertEquals(20L, result.getId());
        verify(chatRepository, times(1)).addParticipant(20L, 1L);
        verify(chatRepository, times(1)).addParticipant(20L, 2L);
    }

    @Test
    public void testGetOrCreateDirectChat_SameUser_ThrowsException() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            chatService.getOrCreateDirectChat(1L, 1L);
        });
        assertEquals("Cannot create a direct chat with yourself", ex.getMessage());
    }

    @Test
    public void testGetOrCreateDirectChat_RecipientNotFound_ThrowsException() {
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            chatService.getOrCreateDirectChat(1L, 99L);
        });
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    public void testSendMessage_Success_BroadcastsWebSocket() {
        Message saved = Message.builder()
                .id(100L)
                .chatId(5L)
                .senderId(1L)
                .ciphertext("encrypted_text")
                .createdAt(LocalDateTime.now())
                .sender(UserResponse.builder().id(1L).username("alice").build())
                .build();

        when(chatRepository.isParticipant(5L, 1L)).thenReturn(true);
        when(chatRepository.saveMessage(any(Message.class))).thenReturn(saved);

        MessageResponse response = chatService.sendMessage(1L, 5L, "encrypted_text");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("encrypted_text", response.getCiphertext());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chats/5"), any(MessageResponse.class));
    }

    @Test
    public void testSendMessage_NotParticipant_ThrowsException() {
        when(chatRepository.isParticipant(5L, 3L)).thenReturn(false);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            chatService.sendMessage(3L, 5L, "encrypted_text");
        });
        assertEquals("You are not a participant in this chat", ex.getMessage());
    }

    @Test
    public void testSendMessage_BlankCiphertext_ThrowsException() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            chatService.sendMessage(1L, 5L, "   ");
        });
        assertEquals("Ciphertext cannot be blank", ex.getMessage());
    }

    @Test
    public void testGetChatById_Success() {
        Chat chat = Chat.builder().id(5L).createdAt(LocalDateTime.now()).build();
        UserResponse u1 = UserResponse.builder().id(1L).username("alice").build();

        when(chatRepository.isParticipant(5L, 1L)).thenReturn(true);
        when(chatRepository.findById(5L)).thenReturn(Optional.of(chat));
        when(chatRepository.findParticipants(5L)).thenReturn(List.of(u1));
        when(chatRepository.findLatestMessage(5L)).thenReturn(Optional.empty());

        ChatResponse response = chatService.getChatById(1L, 5L);
        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals(1, response.getParticipants().size());
    }

    @Test
    public void testGetChatById_NotParticipant_ThrowsException() {
        when(chatRepository.isParticipant(5L, 2L)).thenReturn(false);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            chatService.getChatById(2L, 5L);
        });
        assertEquals("You are not a participant in this chat", ex.getMessage());
    }

    @Test
    public void testGetChatMessages_Success() {
        Message m1 = Message.builder().id(1L).ciphertext("msg1").build();
        Message m2 = Message.builder().id(2L).ciphertext("msg2").build();

        when(chatRepository.isParticipant(5L, 1L)).thenReturn(true);
        when(chatRepository.findMessages(5L, 50, null)).thenReturn(List.of(m1, m2));

        List<MessageResponse> messages = chatService.getChatMessages(1L, 5L, 50, null);
        assertEquals(2, messages.size());
        assertEquals("msg1", messages.get(0).getCiphertext());
    }

    @Test
    public void testLeaveChat_Success() {
        when(chatRepository.isParticipant(5L, 1L)).thenReturn(true);

        chatService.leaveChat(1L, 5L);
        verify(chatRepository, times(1)).removeParticipant(5L, 1L);
    }

    @Test
    public void testLeaveChat_NotParticipant_ThrowsException() {
        when(chatRepository.isParticipant(5L, 99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            chatService.leaveChat(99L, 5L);
        });
        verify(chatRepository, never()).removeParticipant(anyLong(), anyLong());
    }

    @Test
    public void testGetUserChats_Success() {
        Chat c1 = Chat.builder().id(1L).createdAt(LocalDateTime.now()).build();
        Chat c2 = Chat.builder().id(2L).createdAt(LocalDateTime.now()).build();

        when(chatRepository.findChatsByUserId(1L)).thenReturn(List.of(c1, c2));
        when(chatRepository.findParticipants(anyLong())).thenReturn(List.of());
        when(chatRepository.findLatestMessage(anyLong())).thenReturn(Optional.empty());

        List<ChatResponse> chats = chatService.getUserChats(1L);
        assertEquals(2, chats.size());
        assertEquals(1L, chats.get(0).getId());
        assertEquals(2L, chats.get(1).getId());
    }
}
