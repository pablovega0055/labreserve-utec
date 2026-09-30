package com.utec.dbp.dto;

import com.utec.dbp.model.LaboratoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Body de POST /laboratories. status es opcional (por defecto ACTIVE)
public record LaboratoryRequest(
        @NotBlank(message = "name es obligatorio")
        @Size(max = 100, message = "name maximo 100 caracteres")
        String name,

        @NotBlank(message = "location es obligatorio")
        @Size(max = 150, message = "location maximo 150 caracteres")
        String location,

        @NotNull(message = "managerId es obligatorio")
        Long managerId,

        LaboratoryStatus status
) {
}
