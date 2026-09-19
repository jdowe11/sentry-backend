package com.sentry.chat;

import com.sentry.chat.dto.ChatResponse;
import com.sentry.chat.dto.MessageResponse;

import java.util.List;

public interface ChatService {
    ChatResponse getOrCreateDirectChat(Long currentUserId, Long recipientId);
    List<ChatResponse> getUserChats(Long currentUserId);
    ChatResponse getChatById(Long currentUserId, Long chatId);
    MessageResponse sendMessage(Long currentUserId, Long chatId, String ciphertext);
    List<MessageResponse> getChatMessages(Long currentUserId, Long chatId, int limit, Long beforeId);
    void leaveChat(Long currentUserId, Long chatId);
}
