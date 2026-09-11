package com.sentry.chat;

import com.sentry.chat.model.Chat;
import com.sentry.chat.model.Message;
import com.sentry.user.dto.UserResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ChatRepositoryImpl implements ChatRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Chat> chatRowMapper = (rs, rowNum) -> Chat.builder()
            .id(rs.getLong("id"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    private final RowMapper<UserResponse> userResponseRowMapper = (rs, rowNum) -> UserResponse.builder()
            .id(rs.getLong("id"))
            .username(rs.getString("username"))
            .displayName(rs.getString("display_name"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    private final RowMapper<Message> messageRowMapper = (rs, rowNum) -> {
        UserResponse sender = UserResponse.builder()
                .id(rs.getLong("sender_id"))
                .username(rs.getString("sender_username"))
                .displayName(rs.getString("sender_display_name"))
                .createdAt(rs.getTimestamp("sender_created_at").toLocalDateTime())
                .build();

        return Message.builder()
                .id(rs.getLong("id"))
                .chatId(rs.getLong("chat_id"))
                .senderId(rs.getLong("sender_id"))
                .ciphertext(rs.getString("ciphertext"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .sender(sender)
                .build();
    };

    @Override
    public Chat createChat() {
        String sql = "INSERT INTO chats (created_at) VALUES (CURRENT_TIMESTAMP)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> connection.prepareStatement(sql, new String[]{"id"}), keyHolder);

        Number key = keyHolder.getKey();
        Long id = key != null ? key.longValue() : null;
        if (id == null) {
            throw new IllegalStateException("Failed to retrieve generated chat ID");
        }
        return findById(id).orElseThrow(() -> new IllegalStateException("Failed to load newly created chat"));
    }

    @Override
    public void addParticipant(Long chatId, Long userId) {
        String sql = "INSERT INTO chat_participants (chat_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        jdbcTemplate.update(sql, chatId, userId);
    }

    @Override
    public void removeParticipant(Long chatId, Long userId) {
        String sql = "DELETE FROM chat_participants WHERE chat_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, chatId, userId);
    }

    @Override
    public boolean isParticipant(Long chatId, Long userId) {
        String sql = "SELECT COUNT(*) FROM chat_participants WHERE chat_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, chatId, userId);
        return count != null && count > 0;
    }

    @Override
    public Optional<Chat> findDirectChat(Long userId1, Long userId2) {
        String sql = """
            SELECT c.id, c.created_at
            FROM chats c
            JOIN chat_participants p1 ON c.id = p1.chat_id AND p1.user_id = ?
            JOIN chat_participants p2 ON c.id = p2.chat_id AND p2.user_id = ?
            WHERE (SELECT COUNT(*) FROM chat_participants cp WHERE cp.chat_id = c.id) = 2
            LIMIT 1
        """;
        List<Chat> results = jdbcTemplate.query(sql, chatRowMapper, userId1, userId2);
        return results.stream().findFirst();
    }

    @Override
    public Optional<Chat> findById(Long chatId) {
        String sql = "SELECT id, created_at FROM chats WHERE id = ?";
        List<Chat> results = jdbcTemplate.query(sql, chatRowMapper, chatId);
        return results.stream().findFirst();
    }

    @Override
    public List<Chat> findChatsByUserId(Long userId) {
        String sql = """
            SELECT c.id, c.created_at
            FROM chats c
            JOIN chat_participants cp ON c.id = cp.chat_id
            WHERE cp.user_id = ?
            ORDER BY (
                COALESCE(
                    (SELECT MAX(m.created_at) FROM messages m WHERE m.chat_id = c.id),
                    c.created_at
                )
            ) DESC
        """;
        return jdbcTemplate.query(sql, chatRowMapper, userId);
    }

    @Override
    public List<UserResponse> findParticipants(Long chatId) {
        String sql = """
            SELECT u.id, u.username, u.display_name, u.created_at
            FROM users u
            JOIN chat_participants cp ON u.id = cp.user_id
            WHERE cp.chat_id = ?
            ORDER BY cp.joined_at ASC
        """;
        return jdbcTemplate.query(sql, userResponseRowMapper, chatId);
    }

    @Override
    public Message saveMessage(Message message) {
        String sql = "INSERT INTO messages (chat_id, sender_id, ciphertext, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, message.getChatId());
            ps.setLong(2, message.getSenderId());
            ps.setString(3, message.getCiphertext());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            message.setId(key.longValue());
        }
        return findMessageById(message.getId()).orElse(message);
    }

    @Override
    public Optional<Message> findLatestMessage(Long chatId) {
        String sql = """
            SELECT m.id, m.chat_id, m.sender_id, m.ciphertext, m.created_at,
                   u.username as sender_username, u.display_name as sender_display_name, u.created_at as sender_created_at
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.chat_id = ?
            ORDER BY m.created_at DESC, m.id DESC
            LIMIT 1
        """;
        List<Message> results = jdbcTemplate.query(sql, messageRowMapper, chatId);
        return results.stream().findFirst();
    }

    @Override
    public List<Message> findMessages(Long chatId, int limit, Long beforeId) {
        String sql;
        List<Message> messages;
        if (beforeId != null && beforeId > 0) {
            sql = """
                SELECT m.id, m.chat_id, m.sender_id, m.ciphertext, m.created_at,
                       u.username as sender_username, u.display_name as sender_display_name, u.created_at as sender_created_at
                FROM messages m
                JOIN users u ON m.sender_id = u.id
                WHERE m.chat_id = ? AND m.id < ?
                ORDER BY m.id DESC
                LIMIT ?
            """;
            messages = jdbcTemplate.query(sql, messageRowMapper, chatId, beforeId, limit);
        } else {
            sql = """
                SELECT m.id, m.chat_id, m.sender_id, m.ciphertext, m.created_at,
                       u.username as sender_username, u.display_name as sender_display_name, u.created_at as sender_created_at
                FROM messages m
                JOIN users u ON m.sender_id = u.id
                WHERE m.chat_id = ?
                ORDER BY m.id DESC
                LIMIT ?
            """;
            messages = jdbcTemplate.query(sql, messageRowMapper, chatId, limit);
        }
        Collections.reverse(messages);
        return messages;
    }

    private Optional<Message> findMessageById(Long id) {
        String sql = """
            SELECT m.id, m.chat_id, m.sender_id, m.ciphertext, m.created_at,
                   u.username as sender_username, u.display_name as sender_display_name, u.created_at as sender_created_at
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.id = ?
        """;
        List<Message> results = jdbcTemplate.query(sql, messageRowMapper, id);
        return results.stream().findFirst();
    }
}
