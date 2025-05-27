package com.escritr.escritr.auth.controller.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginDTO(
        @NotBlank(message = "username/e-mail cannot be empty")
        @NotNull(message = "username/e-mail is required")
        String login,
        @NotBlank(message = "password cannot be empty")
        @NotNull(message = "password is required")
        String password) {
}
