package com.sentry.friend;

import com.sentry.friend.model.Friendship;
import com.sentry.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class FriendshipRepositoryImpl implements FriendshipRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<UserResponse> userResponseRowMapper = (rs, rowNum) -> UserResponse.builder()
            .id(rs.getLong("id"))
            .username(rs.getString("username"))
            .displayName(rs.getString("display_name"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    private final RowMapper<Friendship> friendshipRowMapper = (rs, rowNum) -> Friendship.builder()
            .userId1(rs.getLong("user_id_1"))
            .userId2(rs.getLong("user_id_2"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    @Override
    public Friendship save(Friendship friendship) {
        long u1 = Math.min(friendship.getUserId1(), friendship.getUserId2());
        long u2 = Math.max(friendship.getUserId1(), friendship.getUserId2());

        if (exists(u1, u2)) {
            String sql = "SELECT * FROM friendships WHERE user_id_1 = ? AND user_id_2 = ?";
            return jdbcTemplate.queryForObject(sql, friendshipRowMapper, u1, u2);
        }

        String sql = "INSERT INTO friendships (user_id_1, user_id_2) VALUES (?, ?)";
        jdbcTemplate.update(sql, u1, u2);

        String selectSql = "SELECT * FROM friendships WHERE user_id_1 = ? AND user_id_2 = ?";
        return jdbcTemplate.queryForObject(selectSql, friendshipRowMapper, u1, u2);
    }

    @Override
    public List<UserResponse> findFriendsByUserId(Long userId) {
        String sql = "SELECT u.id, u.username, u.display_name, u.created_at FROM users u " +
                "JOIN friendships f ON (f.user_id_1 = u.id OR f.user_id_2 = u.id) " +
                "WHERE (f.user_id_1 = ? OR f.user_id_2 = ?) AND u.id <> ?";
        return jdbcTemplate.query(sql, userResponseRowMapper, userId, userId, userId);
    }

    @Override
    public boolean exists(Long userId1, Long userId2) {
        long u1 = Math.min(userId1, userId2);
        long u2 = Math.max(userId1, userId2);

        String sql = "SELECT COUNT(*) FROM friendships WHERE user_id_1 = ? AND user_id_2 = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, u1, u2);
        return count != null && count > 0;
    }

    @Override
    public void delete(Long userId1, Long userId2) {
        long u1 = Math.min(userId1, userId2);
        long u2 = Math.max(userId1, userId2);

        String sql = "DELETE FROM friendships WHERE user_id_1 = ? AND user_id_2 = ?";
        jdbcTemplate.update(sql, u1, u2);
    }
}
