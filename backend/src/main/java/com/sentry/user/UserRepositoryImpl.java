package com.sentry.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.sentry.user.model.User;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class UserRepositoryImpl implements UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> User.builder()
            .id(rs.getLong("id"))
            .username(rs.getString("username"))
            .displayName(rs.getString("display_name"))
            .passwordHash(rs.getString("password_hash"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    @Override
    public User save(User user) {
        if (user.getId() == null || user.getId() <= 0) {
            String sql = "INSERT INTO users (username, display_name, password_hash) VALUES (?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getDisplayName());
                ps.setString(3, user.getPasswordHash());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                user.setId(key.longValue());
            }
            return findById(user.getId()).orElse(user);
        } else {
            String sql = "UPDATE users SET username = ?, display_name = ?, password_hash = ? WHERE id = ?";
            jdbcTemplate.update(sql, user.getUsername(), user.getDisplayName(), user.getPasswordHash(), user.getId());
            return user;
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, id);
        return users.stream().findFirst();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, username);
        return users.stream().findFirst();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public List<User> searchByUsername(String query) {
        String sql = "SELECT * FROM users WHERE LOWER(username) LIKE ?";
        String likeQuery = "%" + query.toLowerCase() + "%";
        return jdbcTemplate.query(sql, userRowMapper, likeQuery);
    }
}
