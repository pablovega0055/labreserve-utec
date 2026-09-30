package com.utec.dbp.service;

import com.utec.dbp.dto.EquipmentSlotRequest;
import com.utec.dbp.dto.EquipmentSlotResponse;
import com.utec.dbp.exception.BadRequestException;
import com.utec.dbp.exception.ConflictException;
import com.utec.dbp.model.*;
import com.utec.dbp.repository.EquipmentSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentSlotServiceTest {

    @Mock
    private EquipmentSlotRepository slotRepository;
    @Mock
    private LaboratoryService laboratoryService;

    @InjectMocks
    private EquipmentSlotService slotService;

    private Laboratory lab;
    private final LocalDateTime start = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUp() {
        User tech = new User("technician", "tech@utec.edu.pe", "hash", Role.TECHNICIAN);
        lab = new Laboratory("FabLab", "Pabellon A", tech, LaboratoryStatus.ACTIVE);
        lab.setId(1L);
    }

    @Test
    void create_ok() {
        when(laboratoryService.getEntity(1L)).thenReturn(lab);
        when(slotRepository.save(any(EquipmentSlot.class))).thenAnswer(inv -> {
            EquipmentSlot s = inv.getArgument(0);
            s.setId(7L);
            return s;
        });

        EquipmentSlotResponse res = slotService.create(
                new EquipmentSlotRequest(1L, "IMP3D-01", start, start.plusHours(2), 3));

        assertEquals(7L, res.id());
        assertEquals("FabLab", res.laboratoryName());
        assertEquals("IMP3D-01", res.equipmentCode());
        assertEquals(SlotStatus.AVAILABLE, res.status());
    }

    @Test
    void create_endAntesDeStart_lanza400() {
        assertThrows(BadRequestException.class, () -> slotService.create(
                new EquipmentSlotRequest(1L, "IMP3D-01", start, start.minusHours(1), 3)));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void create_laboratorioInactivo_lanza400() {
        lab.setStatus(LaboratoryStatus.MAINTENANCE);
        when(laboratoryService.getEntity(1L)).thenReturn(lab);

        assertThrows(BadRequestException.class, () -> slotService.create(
                new EquipmentSlotRequest(1L, "IMP3D-01", start, start.plusHours(2), 3)));
    }

    @Test
    void create_cruceDeHorario_lanza409() {
        when(laboratoryService.getEntity(1L)).thenReturn(lab);
        when(slotRepository.existsByLaboratoryIdAndEquipmentIgnoreCaseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(1L), eq("IMP3D-01"), eq(SlotStatus.CANCELLED), any(), any())).thenReturn(true);

        assertThrows(ConflictException.class, () -> slotService.create(
                new EquipmentSlotRequest(1L, "IMP3D-01", start, start.plusHours(2), 3)));
        verify(slotRepository, never()).save(any());
    }
}
