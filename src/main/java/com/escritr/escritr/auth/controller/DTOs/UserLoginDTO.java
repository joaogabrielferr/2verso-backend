package com.escritr.escritr.auth.controller.DTOs;

import java.util.UUID;

public record UserLoginDTO(UUID id, String username, String email) {
}
