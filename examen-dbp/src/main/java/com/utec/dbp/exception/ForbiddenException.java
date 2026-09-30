package com.utec.dbp.exception;

// -> 403 FORBIDDEN (ej: cancelar la reserva de otro estudiante)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
