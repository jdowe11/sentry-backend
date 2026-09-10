package com.sentry.friend;

import com.sentry.common.TestUserHelper;
import com.sentry.friend.model.FriendRequest;
import com.sentry.user.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import(FriendRequestRepositoryImpl.class)
public class FriendRequestRepositoryTest {

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    public void setUp() {
        // Seed users to satisfy foreign key constraints
        alice = TestUserHelper.insertTestUser(jdbcTemplate, "alice", "Alice");
        bob = TestUserHelper.insertTestUser(jdbcTemplate, "bob", "Bob");
        charlie = TestUserHelper.insertTestUser(jdbcTemplate, "charlie", "Charlie");
    }

    @Test
    public void testSave_Insert() {
        FriendRequest request = FriendRequest.builder()
                .senderId(alice.getId())
                .receiverId(bob.getId())
                .status("pending")
                .build();

        FriendRequest saved = friendRequestRepository.save(request);
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("pending", saved.getStatus());
        assertEquals(alice.getId(), saved.getSenderId());
        assertEquals(bob.getId(), saved.getReceiverId());
    }

    @Test
    public void testSave_Update() {
        FriendRequest request = FriendRequest.builder()
                .senderId(alice.getId())
                .receiverId(bob.getId())
                .status("pending")
                .build();

        FriendRequest saved = friendRequestRepository.save(request);
        saved.setStatus("accepted");

        FriendRequest updated = friendRequestRepository.save(saved);
        assertEquals("accepted", updated.getStatus());
        assertEquals(saved.getId(), updated.getId());
    }

    @Test
    public void testFindById_Success() {
        FriendRequest request = FriendRequest.builder()
                .senderId(alice.getId())
                .receiverId(bob.getId())
                .status("pending")
                .build();
        FriendRequest saved = friendRequestRepository.save(request);

        Optional<FriendRequest> found = friendRequestRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("pending", found.get().getStatus());

        // Verify joined sender and receiver User structures are populated
        assertNotNull(found.get().getSender());
        assertEquals("alice", found.get().getSender().getUsername());
        assertNotNull(found.get().getReceiver());
        assertEquals("bob", found.get().getReceiver().getUsername());
    }

    @Test
    public void testFindById_NotFound() {
        Optional<FriendRequest> found = friendRequestRepository.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    public void testFindBySenderAndReceiver() {
        FriendRequest request = FriendRequest.builder()
                .senderId(alice.getId())
                .receiverId(bob.getId())
                .status("pending")
                .build();
        friendRequestRepository.save(request);

        // Find by exact direction
        Optional<FriendRequest> found1 = friendRequestRepository.findBySenderAndReceiver(alice.getId(), bob.getId());
        assertTrue(found1.isPresent());

        // Find by opposite direction (should still match because findBySenderAndReceiver checks both directions)
        Optional<FriendRequest> found2 = friendRequestRepository.findBySenderAndReceiver(bob.getId(), alice.getId());
        assertTrue(found2.isPresent());
        assertEquals(found1.get().getId(), found2.get().getId());

        // Find nonexistent pair
        Optional<FriendRequest> notFound = friendRequestRepository.findBySenderAndReceiver(alice.getId(), charlie.getId());
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindPendingByUserId() {
        // Alice -> Bob (Pending)
        friendRequestRepository.save(FriendRequest.builder()
                .senderId(alice.getId())
                .receiverId(bob.getId())
                .status("pending")
                .build());

        // Charlie -> Alice (Pending)
        friendRequestRepository.save(FriendRequest.builder()
                .senderId(charlie.getId())
                .receiverId(alice.getId())
                .status("pending")
                .build());

        // Bob -> Charlie (Accepted) - Should not be returned
        friendRequestRepository.save(FriendRequest.builder()
                .senderId(bob.getId())
                .receiverId(charlie.getId())
                .status("accepted")
                .build());

        List<FriendRequest> alicePending = friendRequestRepository.findPendingByUserId(alice.getId());
        assertEquals(2, alicePending.size());

        List<FriendRequest> charliePending = friendRequestRepository.findPendingByUserId(charlie.getId());
        assertEquals(1, charliePending.size());
    }

    @Test
    public void testDeleteById() {
        FriendRequest request = FriendRequest.builder()
                .senderId(alice.getId())
                .receiverId(bob.getId())
                .status("pending")
                .build();
        FriendRequest saved = friendRequestRepository.save(request);

        friendRequestRepository.deleteById(saved.getId());

        Optional<FriendRequest> found = friendRequestRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }

    @Test
    public void testDeleteBySenderIdAndReceiverId() {
        FriendRequest request = FriendRequest.builder()
                .senderId(alice.getId())
                .receiverId(bob.getId())
                .status("pending")
                .build();
        FriendRequest saved = friendRequestRepository.save(request);

        assertTrue(friendRequestRepository.findById(saved.getId()).isPresent());
        friendRequestRepository.deleteBySenderIdAndReceiverId(alice.getId(), bob.getId());
        assertFalse(friendRequestRepository.findById(saved.getId()).isPresent());
    }
}
