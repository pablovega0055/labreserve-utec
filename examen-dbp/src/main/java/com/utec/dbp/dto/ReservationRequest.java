package com.utec.dbp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Body de POST /reservations. El estudiante sale del token JWT, no del body.
public record ReservationRequest(
        @NotNull(message = "slotId es obligatorio")
        Long slotId,

        @NotBlank(message = "purpose es obligatorio")
        @Size(max = 255, message = "purpose maximo 255 caracteres")
        String purpose
) {
}
