package com.sentry.auth;

import com.sentry.user.model.User;

public interface AuthService {
    User login(String username, String password);
}
