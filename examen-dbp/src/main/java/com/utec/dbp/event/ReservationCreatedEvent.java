package com.utec.dbp.event;

import java.time.LocalDateTime;

// Se publica cuando un estudiante reserva un turno
public record ReservationCreatedEvent(
        Long reservationId,
        String studentEmail,
        String laboratoryName,
        String equipmentCode,
        LocalDateTime startTime
) {
}
