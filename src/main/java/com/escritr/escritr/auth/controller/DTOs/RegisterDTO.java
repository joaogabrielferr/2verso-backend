package com.escritr.escritr.auth.controller.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDTO(
        @NotBlank(message = "username cannot be empty")
        @NotNull(message = "username is required")
        String username,
        @NotBlank(message = "e-mail cannot be empty")
        @NotNull(message = "e-mail is required")
        String email,
        @NotBlank(message = "password cannot be empty")
        @NotNull(message = "password is required")
        String password,
        @NotBlank(message = "name cannot be empty")
        @NotNull(message = "name is required")
        String name) {
}
