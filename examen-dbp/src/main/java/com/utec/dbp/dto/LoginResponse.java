package com.utec.dbp.dto;

// Respuesta de POST /auth/login. expiresIn en segundos.
public record LoginResponse(String token, long expiresIn) {
}
