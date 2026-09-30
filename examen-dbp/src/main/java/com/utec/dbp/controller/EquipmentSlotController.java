package com.utec.dbp.controller;

import com.utec.dbp.dto.EquipmentSlotRequest;
import com.utec.dbp.dto.EquipmentSlotResponse;
import com.utec.dbp.dto.PagedResponse;
import com.utec.dbp.model.SlotStatus;
import com.utec.dbp.service.EquipmentSlotService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/equipment-slots")
public class EquipmentSlotController {

    private final EquipmentSlotService slotService;

    public EquipmentSlotController(EquipmentSlotService slotService) {
        this.slotService = slotService;
    }

    // POST /equipment-slots -> 201 (publicar turno: TECHNICIAN o ADMIN)
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @PostMapping
    public ResponseEntity<EquipmentSlotResponse> create(@Valid @RequestBody EquipmentSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slotService.create(request));
    }

    // GET /equipment-slots?page=0&size=10&laboratoryId=1&status=AVAILABLE
    // -> { content: [{ id, laboratoryName, equipmentCode, ... }], page, size, totalElements }
    @GetMapping
    public ResponseEntity<PagedResponse<EquipmentSlotResponse>> search(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) SlotStatus status,
            @PageableDefault(page = 0, size = 10, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(slotService.search(laboratoryId, status, pageable)));
    }
}
