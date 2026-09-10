package com.sentry.user;

import com.sentry.user.dto.CreateUserRequest;
import com.sentry.user.dto.UpdateDisplayNameRequest;
import com.sentry.user.dto.UpdateUsernameRequest;
import com.sentry.user.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User createUser(CreateUserRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = User.builder()
                .username(request.getUsername().trim())
                .displayName(request.getDisplayName().trim())
                .passwordHash(request.getPasswordHash())
                .build();
        return userRepository.save(user);
    }

    @Override
    public User createUser(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (userRepository.existsByUsername(user.getUsername().trim())) {
            throw new IllegalArgumentException("Username already exists");
        }
        return userRepository.save(user);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        return userRepository.findByUsername(username.trim());
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        userRepository.deleteById(id);
    }

    @Override
    public User updateUsername(Long id, UpdateUsernameRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newUsername = request.getNewUsername().trim();
        if (!existing.getUsername().equals(newUsername)) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new IllegalArgumentException("Username is already taken");
            }
            existing.setUsername(newUsername);
        }

        return userRepository.save(existing);
    }

    @Override
    public User updateDisplayName(Long id, UpdateDisplayNameRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newDisplayName = request.getNewDisplayName().trim();
        existing.setDisplayName(newDisplayName);

        return userRepository.save(existing);
    }

    @Override
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return userRepository.searchByUsername(query.trim());
    }
}
