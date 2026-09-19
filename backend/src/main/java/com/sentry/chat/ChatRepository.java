package com.sentry.chat;

import com.sentry.chat.model.Chat;
import com.sentry.chat.model.Message;
import com.sentry.user.dto.UserResponse;

import java.util.List;
import java.util.Optional;

public interface ChatRepository {
    Chat createChat();
    void addParticipant(Long chatId, Long userId);
    void removeParticipant(Long chatId, Long userId);
    boolean isParticipant(Long chatId, Long userId);
    Optional<Chat> findDirectChat(Long userId1, Long userId2);
    Optional<Chat> findById(Long chatId);
    List<Chat> findChatsByUserId(Long userId);
    List<UserResponse> findParticipants(Long chatId);
    Message saveMessage(Message message);
    List<Message> findMessages(Long chatId, int limit, Long beforeId);
    Optional<Message> findLatestMessage(Long chatId);
}
