package com.utec.dbp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body de POST /auth/register
public record RegisterRequest(
        @NotBlank(message = "username es obligatorio")
        @Size(min = 3, max = 50, message = "username debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "email es obligatorio")
        @Email(message = "email no tiene un formato valido")
        String email,

        @NotBlank(message = "password es obligatorio")
        @Size(min = 8, message = "password debe tener al menos 8 caracteres")
        String password
) {
}
