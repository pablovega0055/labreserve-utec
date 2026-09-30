package com.utec.dbp.dto;

import com.utec.dbp.model.User;

// Respuesta de POST /auth/register (nunca se devuelve el password)
public record UserResponse(Long id, String username, String email) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail());
    }
}
