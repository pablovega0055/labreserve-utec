package com.utec.dbp.dto;

import com.utec.dbp.model.Laboratory;
import com.utec.dbp.model.LaboratoryStatus;

public record LaboratoryResponse(
        Long id,
        String name,
        String location,
        Long managerId,
        LaboratoryStatus status
) {
    public static LaboratoryResponse from(Laboratory l) {
        return new LaboratoryResponse(l.getId(), l.getName(), l.getLocation(),
                l.getManager().getId(), l.getStatus());
    }
}
