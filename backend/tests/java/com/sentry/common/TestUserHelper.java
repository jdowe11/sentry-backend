package com.sentry.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.sentry.user.model.User;

import java.sql.PreparedStatement;

public class TestUserHelper {

    public static final String DEFAULT_PASSWORD_HASH = "test_password_hash";

    /**
     * Inserts a test user into the database and returns the populated User entity with its generated ID.
     */
    public static User insertTestUser(JdbcTemplate jdbcTemplate, String username, String displayName, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (username, display_name, password_hash) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, username);
            ps.setString(2, displayName);
            ps.setString(3, passwordHash);
            return ps;
        }, keyHolder);

        return User.builder()
                .id(keyHolder.getKey().longValue())
                .username(username)
                .displayName(displayName)
                .passwordHash(passwordHash)
                .build();
    }

    /**
     * Overload using the default test password hash.
     */
    public static User insertTestUser(JdbcTemplate jdbcTemplate, String username, String displayName) {
        return insertTestUser(jdbcTemplate, username, displayName, DEFAULT_PASSWORD_HASH);
    }
}
