package com.utec.dbp.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

// Body de POST /equipment-slots (publicar turno)
public record EquipmentSlotRequest(
        @NotNull(message = "laboratoryId es obligatorio")
        Long laboratoryId,

        @NotBlank(message = "equipmentCode es obligatorio")
        @Size(max = 50, message = "equipmentCode maximo 50 caracteres")
        String equipmentCode,

        @NotNull(message = "startTime es obligatorio")
        @Future(message = "startTime debe ser una fecha futura")
        LocalDateTime startTime,

        @NotNull(message = "endTime es obligatorio")
        @Future(message = "endTime debe ser una fecha futura")
        LocalDateTime endTime,

        @NotNull(message = "capacity es obligatorio")
        @Min(value = 1, message = "capacity minimo 1")
        @Max(value = 50, message = "capacity maximo 50")
        Integer capacity
) {
}
