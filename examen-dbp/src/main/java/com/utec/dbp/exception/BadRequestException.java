package com.utec.dbp.exception;

// -> 400 BAD REQUEST (ej: stock insuficiente, regla de negocio violada)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
