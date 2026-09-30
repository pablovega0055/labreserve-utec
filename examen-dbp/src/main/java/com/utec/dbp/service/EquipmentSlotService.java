package com.utec.dbp.service;

import com.utec.dbp.dto.EquipmentSlotRequest;
import com.utec.dbp.dto.EquipmentSlotResponse;
import com.utec.dbp.exception.BadRequestException;
import com.utec.dbp.exception.ConflictException;
import com.utec.dbp.model.EquipmentSlot;
import com.utec.dbp.model.Laboratory;
import com.utec.dbp.model.LaboratoryStatus;
import com.utec.dbp.model.SlotStatus;
import com.utec.dbp.repository.EquipmentSlotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentSlotService {

    private final EquipmentSlotRepository slotRepository;
    private final LaboratoryService laboratoryService;

    public EquipmentSlotService(EquipmentSlotRepository slotRepository, LaboratoryService laboratoryService) {
        this.slotRepository = slotRepository;
        this.laboratoryService = laboratoryService;
    }

    // Publicar turno
    @Transactional
    public EquipmentSlotResponse create(EquipmentSlotRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BadRequestException("endTime debe ser posterior a startTime");
        }

        Laboratory lab = laboratoryService.getEntity(req.laboratoryId());   // 404 si no existe
        if (lab.getStatus() != LaboratoryStatus.ACTIVE) {
            throw new BadRequestException("El laboratorio no esta activo: " + lab.getStatus());
        }

        String equipmentCode = req.equipmentCode().trim();
        boolean overlaps = slotRepository
                .existsByLaboratoryIdAndEquipmentIgnoreCaseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        lab.getId(), equipmentCode, SlotStatus.CANCELLED, req.endTime(), req.startTime());
        if (overlaps) {
            throw new ConflictException("El equipo " + equipmentCode + " ya tiene un turno en ese horario");
        }

        EquipmentSlot slot = new EquipmentSlot(lab, equipmentCode, req.startTime(), req.endTime(), req.capacity());
        return EquipmentSlotResponse.from(slotRepository.save(slot));
    }

    // Buscar turnos (filtros opcionales por laboratorio y estado)
    @Transactional(readOnly = true)
    public Page<EquipmentSlotResponse> search(Long laboratoryId, SlotStatus status, Pageable pageable) {
        Page<EquipmentSlot> page;
        if (laboratoryId != null && status != null) {
            page = slotRepository.findByLaboratoryIdAndStatus(laboratoryId, status, pageable);
        } else if (laboratoryId != null) {
            page = slotRepository.findByLaboratoryId(laboratoryId, pageable);
        } else if (status != null) {
            page = slotRepository.findByStatus(status, pageable);
        } else {
            page = slotRepository.findAll(pageable);
        }
        return page.map(EquipmentSlotResponse::from);
    }
}
