package com.utec.dbp.dto;

import com.utec.dbp.model.LabReservation;
import com.utec.dbp.model.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long slotId,
        String laboratoryName,
        String equipmentCode,
        Long studentId,
        String purpose,
        LocalDateTime reservedAt,
        ReservationStatus status
) {
    public static ReservationResponse from(LabReservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getSlot().getId(),
                r.getSlot().getLaboratory().getName(),
                r.getSlot().getEquipment(),
                r.getStudent().getId(),
                r.getPurpose(),
                r.getReservedAt(),
                r.getStatus());
    }
}
