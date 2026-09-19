package com.sentry.chat;

import com.sentry.chat.dto.ChatResponse;
import com.sentry.chat.dto.MessageResponse;
import com.sentry.chat.model.Chat;
import com.sentry.chat.model.Message;
import com.sentry.user.UserService;
import com.sentry.user.dto.UserResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final UserService userService;
    private final SimpMessageSendingOperations messagingTemplate;

    @Override
    @Transactional
    public ChatResponse getOrCreateDirectChat(Long currentUserId, Long recipientId) {
        if (recipientId == null) {
            throw new IllegalArgumentException("Recipient ID cannot be null");
        }
        if (currentUserId.equals(recipientId)) {
            throw new IllegalArgumentException("Cannot create a direct chat with yourself");
        }

        userService.getUserById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient user with ID " + recipientId + " does not exist"));

        Optional<Chat> existingChat = chatRepository.findDirectChat(currentUserId, recipientId);
        if (existingChat.isPresent()) {
            return toChatResponse(existingChat.get());
        }

        Chat newChat = chatRepository.createChat();
        chatRepository.addParticipant(newChat.getId(), currentUserId);
        chatRepository.addParticipant(newChat.getId(), recipientId);

        return toChatResponse(newChat);
    }

    @Override
    public List<ChatResponse> getUserChats(Long currentUserId) {
        List<Chat> chats = chatRepository.findChatsByUserId(currentUserId);
        return chats.stream()
                .map(this::toChatResponse)
                .toList();
    }

    @Override
    public ChatResponse getChatById(Long currentUserId, Long chatId) {
        validateMembership(chatId, currentUserId);
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with ID: " + chatId));
        return toChatResponse(chat);
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long currentUserId, Long chatId, String ciphertext) {
        if (ciphertext == null || ciphertext.trim().isEmpty()) {
            throw new IllegalArgumentException("Ciphertext cannot be blank");
        }
        validateMembership(chatId, currentUserId);

        Message message = Message.builder()
                .chatId(chatId)
                .senderId(currentUserId)
                .ciphertext(ciphertext)
                .build();

        Message saved = chatRepository.saveMessage(message);
        MessageResponse response = toMessageResponse(saved);

        // Broadcast to WebSocket subscribers for real-time delivery
        try {
            messagingTemplate.convertAndSend("/topic/chats/" + chatId, response);
        } catch (Exception e) {
            log.warn("Failed to broadcast chat message to WebSocket topic /topic/chats/{}: {}", chatId, e.getMessage());
        }

        return response;
    }

    @Override
    public List<MessageResponse> getChatMessages(Long currentUserId, Long chatId, int limit, Long beforeId) {
        validateMembership(chatId, currentUserId);
        int effectiveLimit = Math.max(1, Math.min(limit, 100));

        List<Message> messages = chatRepository.findMessages(chatId, effectiveLimit, beforeId);
        return messages.stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    @Transactional
    public void leaveChat(Long currentUserId, Long chatId) {
        validateMembership(chatId, currentUserId);
        chatRepository.removeParticipant(chatId, currentUserId);
    }

    private void validateMembership(Long chatId, Long userId) {
        if (!chatRepository.isParticipant(chatId, userId)) {
            throw new IllegalArgumentException("You are not a participant in this chat");
        }
    }

    private ChatResponse toChatResponse(Chat chat) {
        List<UserResponse> participants = chatRepository.findParticipants(chat.getId());
        Message lastMessage = chatRepository.findLatestMessage(chat.getId()).orElse(null);
        MessageResponse lastMessageResponse = lastMessage != null ? toMessageResponse(lastMessage) : null;

        return ChatResponse.builder()
                .id(chat.getId())
                .createdAt(chat.getCreatedAt())
                .participants(participants)
                .lastMessage(lastMessageResponse)
                .build();
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChatId())
                .senderId(message.getSenderId())
                .ciphertext(message.getCiphertext())
                .createdAt(message.getCreatedAt())
                .sender(message.getSender())
                .build();
    }
}
