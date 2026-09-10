package com.sentry.user;

import java.util.Optional;

import com.sentry.user.model.User;

import java.util.List;

interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    boolean existsByUsername(String username);
    void deleteById(Long id);
    List<User> searchByUsername(String query);
}
