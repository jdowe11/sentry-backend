package com.sentry.friend;

import com.sentry.common.TestUserHelper;
import com.sentry.friend.model.Friendship;
import com.sentry.user.dto.UserResponse;
import com.sentry.user.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import(FriendshipRepositoryImpl.class)
public class FriendshipRepositoryTest {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    public void setUp() {
        // Insert test users
        alice = TestUserHelper.insertTestUser(jdbcTemplate, "alice", "Alice");
        bob = TestUserHelper.insertTestUser(jdbcTemplate, "bob", "Bob");
        charlie = TestUserHelper.insertTestUser(jdbcTemplate, "charlie", "Charlie");
    }

    @Test
    public void testSaveAndExists() {
        Friendship f = Friendship.builder()
                .userId1(alice.getId())
                .userId2(bob.getId())
                .build();

        Friendship saved = friendshipRepository.save(f);
        assertNotNull(saved);
        // Assert canonical ordering userId1 < userId2
        long expectedMin = Math.min(alice.getId(), bob.getId());
        long expectedMax = Math.max(alice.getId(), bob.getId());
        assertEquals(expectedMin, saved.getUserId1());
        assertEquals(expectedMax, saved.getUserId2());
        assertNotNull(saved.getCreatedAt());

        // Check exists
        assertTrue(friendshipRepository.exists(alice.getId(), bob.getId()));
        assertTrue(friendshipRepository.exists(bob.getId(), alice.getId()));
    }

    @Test
    public void testFindFriendsByUserId() {
        Friendship f1 = Friendship.builder().userId1(alice.getId()).userId2(bob.getId()).build();
        Friendship f2 = Friendship.builder().userId1(alice.getId()).userId2(charlie.getId()).build();
        friendshipRepository.save(f1);
        friendshipRepository.save(f2);

        List<UserResponse> aliceFriends = friendshipRepository.findFriendsByUserId(alice.getId());
        assertEquals(2, aliceFriends.size());
        assertTrue(aliceFriends.stream().anyMatch(u -> u.getUsername().equals("bob")));
        assertTrue(aliceFriends.stream().anyMatch(u -> u.getUsername().equals("charlie")));

        List<UserResponse> bobFriends = friendshipRepository.findFriendsByUserId(bob.getId());
        assertEquals(1, bobFriends.size());
        assertEquals("alice", bobFriends.get(0).getUsername());
    }

    @Test
    public void testDelete() {
        Friendship f = Friendship.builder().userId1(alice.getId()).userId2(bob.getId()).build();
        friendshipRepository.save(f);

        assertTrue(friendshipRepository.exists(alice.getId(), bob.getId()));
        friendshipRepository.delete(alice.getId(), bob.getId());
        assertFalse(friendshipRepository.exists(alice.getId(), bob.getId()));
    }
}
