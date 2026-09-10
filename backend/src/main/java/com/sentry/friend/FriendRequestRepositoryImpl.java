package com.sentry.friend;

import com.sentry.friend.model.FriendRequest;
import com.sentry.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class FriendRequestRepositoryImpl implements FriendRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<FriendRequest> friendRequestRowMapper = (rs, rowNum) -> {
        UserResponse sender = UserResponse.builder()
                .id(rs.getLong("sender_id"))
                .username(rs.getString("sender_username"))
                .displayName(rs.getString("sender_display_name"))
                .createdAt(rs.getTimestamp("sender_created_at").toLocalDateTime())
                .build();

        UserResponse receiver = UserResponse.builder()
                .id(rs.getLong("receiver_id"))
                .username(rs.getString("receiver_username"))
                .displayName(rs.getString("receiver_display_name"))
                .createdAt(rs.getTimestamp("receiver_created_at").toLocalDateTime())
                .build();

        return FriendRequest.builder()
                .id(rs.getLong("id"))
                .senderId(rs.getLong("sender_id"))
                .receiverId(rs.getLong("receiver_id"))
                .status(rs.getString("status"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .sender(sender)
                .receiver(receiver)
                .build();
    };

    @Override
    public FriendRequest save(FriendRequest request) {
        if (request.getId() == null || request.getId() <= 0) {
            String sql = "INSERT INTO friend_requests (sender_id, receiver_id, status) VALUES (?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setLong(1, request.getSenderId());
                ps.setLong(2, request.getReceiverId());
                ps.setString(3, request.getStatus());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                request.setId(key.longValue());
            }
            return findById(request.getId()).orElse(request);
        } else {
            String sql = "UPDATE friend_requests SET sender_id = ?, receiver_id = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            jdbcTemplate.update(sql, request.getSenderId(), request.getReceiverId(), request.getStatus(), request.getId());
            return findById(request.getId()).orElse(request);
        }
    }

    @Override
    public Optional<FriendRequest> findById(Long id) {
        String sql = "SELECT fr.*, " +
                "su.username as sender_username, su.display_name as sender_display_name, su.created_at as sender_created_at, " +
                "ru.username as receiver_username, ru.display_name as receiver_display_name, ru.created_at as receiver_created_at " +
                "FROM friend_requests fr " +
                "JOIN users su ON fr.sender_id = su.id " +
                "JOIN users ru ON fr.receiver_id = ru.id " +
                "WHERE fr.id = ?";
        List<FriendRequest> requests = jdbcTemplate.query(sql, friendRequestRowMapper, id);
        return requests.stream().findFirst();
    }

    @Override
    public Optional<FriendRequest> findBySenderAndReceiver(Long senderId, Long receiverId) {
        String sql = "SELECT fr.*, " +
                "su.username as sender_username, su.display_name as sender_display_name, su.created_at as sender_created_at, " +
                "ru.username as receiver_username, ru.display_name as receiver_display_name, ru.created_at as receiver_created_at " +
                "FROM friend_requests fr " +
                "JOIN users su ON fr.sender_id = su.id " +
                "JOIN users ru ON fr.receiver_id = ru.id " +
                "WHERE (fr.sender_id = ? AND fr.receiver_id = ?) OR (fr.sender_id = ? AND fr.receiver_id = ?)";
        List<FriendRequest> requests = jdbcTemplate.query(sql, friendRequestRowMapper, senderId, receiverId, receiverId, senderId);
        return requests.stream().findFirst();
    }

    @Override
    public List<FriendRequest> findPendingByUserId(Long userId) {
        String sql = "SELECT fr.*, " +
                "su.username as sender_username, su.display_name as sender_display_name, su.created_at as sender_created_at, " +
                "ru.username as receiver_username, ru.display_name as receiver_display_name, ru.created_at as receiver_created_at " +
                "FROM friend_requests fr " +
                "JOIN users su ON fr.sender_id = su.id " +
                "JOIN users ru ON fr.receiver_id = ru.id " +
                "WHERE (fr.sender_id = ? OR fr.receiver_id = ?) AND fr.status = 'pending'";
        return jdbcTemplate.query(sql, friendRequestRowMapper, userId, userId);
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM friend_requests WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void deleteBySenderIdAndReceiverId(Long senderId, Long receiverId) {
        String sql = "DELETE FROM friend_requests WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)";
        jdbcTemplate.update(sql, senderId, receiverId, receiverId, senderId);
    }
}
