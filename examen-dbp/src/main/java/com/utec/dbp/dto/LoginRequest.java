package com.utec.dbp.dto;

import jakarta.validation.constraints.NotBlank;

// Body de POST /auth/login
public record LoginRequest(
        @NotBlank(message = "username es obligatorio")
        String username,

        @NotBlank(message = "password es obligatorio")
        String password
) {
}
