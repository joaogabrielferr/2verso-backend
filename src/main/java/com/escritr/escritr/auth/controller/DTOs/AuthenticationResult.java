package com.escritr.escritr.auth.controller.DTOs;

import com.escritr.escritr.user.domain.User;

public record AuthenticationResult(
        String accessToken,
        String refreshTokenValue, // Just the string value, controller handles cookie creation,
        User user
) {}