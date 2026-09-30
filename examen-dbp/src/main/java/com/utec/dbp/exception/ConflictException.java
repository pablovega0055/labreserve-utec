package com.utec.dbp.exception;

// -> 409 CONFLICT (ej: nombre/email duplicado)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
