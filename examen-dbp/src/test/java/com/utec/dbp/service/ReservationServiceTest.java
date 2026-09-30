package com.utec.dbp.service;

import com.utec.dbp.dto.ReservationRequest;
import com.utec.dbp.dto.ReservationResponse;
import com.utec.dbp.event.ReservationCreatedEvent;
import com.utec.dbp.exception.BadRequestException;
import com.utec.dbp.exception.ConflictException;
import com.utec.dbp.exception.ForbiddenException;
import com.utec.dbp.exception.ResourceNotFoundException;
import com.utec.dbp.model.*;
import com.utec.dbp.repository.EquipmentSlotRepository;
import com.utec.dbp.repository.LabReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private LabReservationRepository reservationRepository;
    @Mock
    private EquipmentSlotRepository slotRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReservationService reservationService;

    private User student;
    private EquipmentSlot slot;

    @BeforeEach
    void setUp() {
        student = new User("pablo.vega", "pablo.vega@utec.edu.pe", "hash", Role.STUDENT);
        student.setId(10L);

        User tech = new User("technician", "tech@utec.edu.pe", "hash", Role.TECHNICIAN);
        tech.setId(2L);
        Laboratory lab = new Laboratory("FabLab", "Pabellon A", tech, LaboratoryStatus.ACTIVE);
        lab.setId(1L);

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        slot = new EquipmentSlot(lab, "IMP3D-01", start, start.plusHours(2), 2);
        slot.setId(5L);
    }

    private void stubSave() {
        when(reservationRepository.save(any(LabReservation.class))).thenAnswer(inv -> {
            LabReservation r = inv.getArgument(0);
            r.setId(100L);
            r.setReservedAt(LocalDateTime.now());
            return r;
        });
    }

    @Test
    void reserve_ok_publicaEvento() {
        when(slotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(slot));
        when(reservationRepository.countBySlotIdAndStatus(5L, ReservationStatus.CONFIRMED)).thenReturn(0L);
        stubSave();

        ReservationResponse res = reservationService.reserve(new ReservationRequest(5L, "Prototipo"), student);

        assertEquals(100L, res.id());
        assertEquals("FabLab", res.laboratoryName());
        assertEquals("IMP3D-01", res.equipmentCode());
        assertEquals(ReservationStatus.CONFIRMED, res.status());
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
        verify(eventPublisher).publishEvent(any(ReservationCreatedEvent.class));
    }

    @Test
    void reserve_ultimoCupo_marcaTurnoFull() {
        when(slotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(slot));
        when(reservationRepository.countBySlotIdAndStatus(5L, ReservationStatus.CONFIRMED)).thenReturn(1L);
        stubSave();

        reservationService.reserve(new ReservationRequest(5L, "Prototipo"), student);

        assertEquals(SlotStatus.FULL, slot.getStatus());
    }

    @Test
    void reserve_sinCupos_lanza409() {
        when(slotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(slot));
        when(reservationRepository.countBySlotIdAndStatus(5L, ReservationStatus.CONFIRMED)).thenReturn(2L);

        assertThrows(ConflictException.class,
                () -> reservationService.reserve(new ReservationRequest(5L, "Prototipo"), student));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserve_duplicada_lanza409() {
        when(slotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsBySlotIdAndStudentIdAndStatus(5L, 10L, ReservationStatus.CONFIRMED))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> reservationService.reserve(new ReservationRequest(5L, "Prototipo"), student));
    }

    @Test
    void reserve_turnoPasado_lanza400() {
        slot.setStartTime(LocalDateTime.now().minusHours(1));
        when(slotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(slot));

        assertThrows(BadRequestException.class,
                () -> reservationService.reserve(new ReservationRequest(5L, "Prototipo"), student));
    }

    @Test
    void reserve_turnoNoExiste_lanza404() {
        when(slotRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.reserve(new ReservationRequest(99L, "Prototipo"), student));
    }

    @Test
    void cancel_reservaDeOtroEstudiante_lanza403() {
        User otro = new User("otro", "otro@utec.edu.pe", "hash", Role.STUDENT);
        otro.setId(11L);
        LabReservation r = new LabReservation(slot, student, "Prototipo");
        r.setId(100L);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(r));

        assertThrows(ForbiddenException.class, () -> reservationService.cancel(100L, otro));
    }

    @Test
    void cancel_ok_liberaCupo() {
        slot.setStatus(SlotStatus.FULL);
        LabReservation r = new LabReservation(slot, student, "Prototipo");
        r.setId(100L);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(r));

        ReservationResponse res = reservationService.cancel(100L, student);

        assertEquals(ReservationStatus.CANCELLED, res.status());
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
    }
}
