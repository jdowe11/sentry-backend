package com.sentry.chat;

import com.sentry.chat.dto.ChatResponse;
import com.sentry.chat.dto.CreateDirectChatRequest;
import com.sentry.chat.dto.MessageResponse;
import com.sentry.chat.dto.SendMessageRequest;
import com.sentry.common.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1.0/chats")
@Validated
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /// Retrieve or create a direct 1-on-1 chat with a recipient
    @PostMapping("/direct")
    public ResponseEntity<ChatResponse> getOrCreateDirectChat(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Valid @RequestBody CreateDirectChatRequest request
    ) {
        ChatResponse response = chatService.getOrCreateDirectChat(userId, request.getRecipientId());
        return ResponseEntity.ok(response);
    }

    /// Retrieve all chats the current user participates in
    @GetMapping
    public ResponseEntity<List<ChatResponse>> getUserChats(
            @Parameter(hidden = true) @CurrentUserId Long userId
    ) {
        List<ChatResponse> chats = chatService.getUserChats(userId);
        return ResponseEntity.ok(chats);
    }

    /// Retrieve details and participants for a specific chat
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChatById(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Min(1) Long chatId
    ) {
        ChatResponse chat = chatService.getChatById(userId, chatId);
        return ResponseEntity.ok(chat);
    }

    /// Send an encrypted message to a chat
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Min(1) Long chatId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        MessageResponse response = chatService.sendMessage(userId, chatId, request.getCiphertext());
        return ResponseEntity.ok(response);
    }

    /// Retrieve messages for a chat with optional pagination
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageResponse>> getChatMessages(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Min(1) Long chatId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long before
    ) {
        List<MessageResponse> messages = chatService.getChatMessages(userId, chatId, limit, before);
        return ResponseEntity.ok(messages);
    }

    /// Leave a chat
    @DeleteMapping("/{chatId}/participants/me")
    public ResponseEntity<Void> leaveChat(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Min(1) Long chatId
    ) {
        chatService.leaveChat(userId, chatId);
        return ResponseEntity.ok().build();
    }
}
