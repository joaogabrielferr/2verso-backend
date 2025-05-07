package com.escritr.escritr.auth.controller.DTOs;

public record AuthenticationResult(
        String accessToken,
        String refreshTokenValue // Just the string value, controller handles cookie creation
) {}