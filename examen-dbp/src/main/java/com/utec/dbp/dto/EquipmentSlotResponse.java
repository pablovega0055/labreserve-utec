package com.utec.dbp.dto;

import com.utec.dbp.model.EquipmentSlot;
import com.utec.dbp.model.SlotStatus;

import java.time.LocalDateTime;

// Item de GET /equipment-slots -> { id, laboratoryName: "FabLab", equipmentCode, ... }
public record EquipmentSlotResponse(
        Long id,
        String laboratoryName,
        String equipmentCode,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer capacity,
        SlotStatus status
) {
    public static EquipmentSlotResponse from(EquipmentSlot s) {
        return new EquipmentSlotResponse(
                s.getId(), s.getLaboratory().getName(), s.getEquipment(),
                s.getStartTime(), s.getEndTime(), s.getCapacity(), s.getStatus());
    }
}
