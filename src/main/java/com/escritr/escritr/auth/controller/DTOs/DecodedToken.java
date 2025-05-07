package com.escritr.escritr.auth.controller.DTOs;

import java.util.UUID;

public record DecodedToken(
        UUID userId,
        int tokenVersion,
        String username,
        String email
) {}