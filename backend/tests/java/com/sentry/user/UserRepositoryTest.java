package com.sentry.user;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;

import com.sentry.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import(UserRepositoryImpl.class)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testSave_Insert() {
        User user = User.builder()
                .username("dbuser")
                .displayName("DB User")
                .passwordHash("pwd")
                .build();

        User saved = userRepository.save(user);
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);
        assertEquals("dbuser", saved.getUsername());
        
        // Verify in DB
        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("dbuser", found.get().getUsername());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    public void testSave_Update() {
        User user = User.builder()
                .username("dbuser")
                .displayName("DB User")
                .passwordHash("pwd")
                .build();
        User saved = userRepository.save(user);

        saved.setDisplayName("Updated Name");
        User updated = userRepository.save(saved);

        assertEquals("Updated Name", updated.getDisplayName());
        
        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getDisplayName());
    }

    @Test
    public void testFindById_NotFound() {
        Optional<User> found = userRepository.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    public void testFindByUsername() {
        User user = User.builder()
                .username("findme")
                .displayName("Find")
                .passwordHash("pwd")
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("findme");
        assertTrue(found.isPresent());
        assertEquals("findme", found.get().getUsername());

        Optional<User> notFound = userRepository.findByUsername("nonexistent");
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindAll() {
        User u1 = User.builder().username("u1").displayName("User 1").passwordHash("pwd").build();
        User u2 = User.builder().username("u2").displayName("User 2").passwordHash("pwd").build();
        userRepository.save(u1);
        userRepository.save(u2);

        List<User> all = userRepository.findAll();
        assertEquals(2, all.size());
    }

    @Test
    public void testDeleteById() {
        User user = User.builder()
                .username("delete")
                .displayName("Delete")
                .passwordHash("pwd")
                .build();
        User saved = userRepository.save(user);

        userRepository.deleteById(saved.getId());

        Optional<User> found = userRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }

    @Test
    public void testExistsByUsername() {
        User user = User.builder()
                .username("exists")
                .displayName("Exists")
                .passwordHash("pwd")
                .build();
        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("exists"));
        assertFalse(userRepository.existsByUsername("doesnotexist"));
    }

    @Test
    public void testSearchByUsername_Success() {
        User u1 = User.builder().username("alice").displayName("Alice").passwordHash("pwd").build();
        User u2 = User.builder().username("bob").displayName("Bob").passwordHash("pwd").build();
        User u3 = User.builder().username("charlie").displayName("Charlie").passwordHash("pwd").build();
        userRepository.save(u1);
        userRepository.save(u2);
        userRepository.save(u3);

        // Substring case-insensitive match: "li" should match "alice" and "charlie"
        List<User> matches1 = userRepository.searchByUsername("li");
        assertEquals(2, matches1.size());
        assertTrue(matches1.stream().anyMatch(u -> u.getUsername().equals("alice")));
        assertTrue(matches1.stream().anyMatch(u -> u.getUsername().equals("charlie")));

        // Case-insensitivity match: "BO" should match "bob"
        List<User> matches2 = userRepository.searchByUsername("BO");
        assertEquals(1, matches2.size());
        assertEquals("bob", matches2.get(0).getUsername());
    }

    @Test
    public void testSearchByUsername_NoMatches() {
        List<User> matches = userRepository.searchByUsername("nonexistentquery");
        assertTrue(matches.isEmpty());
    }
}
