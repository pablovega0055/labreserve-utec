package com.utec.dbp.repository;

import com.utec.dbp.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

// Test de repositorio con H2 en memoria
@DataJpaTest
class EquipmentSlotRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LaboratoryRepository laboratoryRepository;
    @Autowired
    private EquipmentSlotRepository slotRepository;

    private Laboratory fablab;
    private final LocalDateTime base = LocalDateTime.now().plusDays(1).withNano(0);

    @BeforeEach
    void setUp() {
        User tech = userRepository.save(new User("technician", "tech@utec.edu.pe", "hash", Role.TECHNICIAN));
        fablab = laboratoryRepository.save(new Laboratory("FabLab", "Pabellon A", tech, LaboratoryStatus.ACTIVE));
        Laboratory robotica = laboratoryRepository.save(
                new Laboratory("Robotica", "Pabellon B", tech, LaboratoryStatus.ACTIVE));

        slotRepository.save(new EquipmentSlot(fablab, "IMP3D-01", base, base.plusHours(2), 2));
        slotRepository.save(new EquipmentSlot(fablab, "LASER-01", base, base.plusHours(1), 1));
        slotRepository.save(new EquipmentSlot(robotica, "ARM-01", base, base.plusHours(1), 3));
    }

    @Test
    void findByLaboratoryId_pagina() {
        Page<EquipmentSlot> page = slotRepository.findByLaboratoryId(fablab.getId(), PageRequest.of(0, 1));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals("FabLab", page.getContent().get(0).getLaboratory().getName());
    }

    @Test
    void detectaCruceDeHorario() {
        // 10:30-11:30 se cruza con IMP3D-01 de 10:00-12:00 (base = 10:00)
        assertTrue(slotRepository
                .existsByLaboratoryIdAndEquipmentIgnoreCaseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        fablab.getId(), "imp3d-01", SlotStatus.CANCELLED,
                        base.plusMinutes(90), base.plusMinutes(30)));

        // 12:00-13:00 empieza justo cuando termina el otro -> no hay cruce
        assertFalse(slotRepository
                .existsByLaboratoryIdAndEquipmentIgnoreCaseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        fablab.getId(), "IMP3D-01", SlotStatus.CANCELLED,
                        base.plusHours(3), base.plusHours(2)));
    }

    @Test
    void usernameYEmailUnicos() {
        assertTrue(userRepository.existsByUsername("technician"));
        assertTrue(userRepository.existsByEmail("tech@utec.edu.pe"));
        assertFalse(userRepository.existsByUsername("nadie"));
    }
}
