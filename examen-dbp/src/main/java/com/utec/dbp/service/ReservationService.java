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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReservationService {

    private final LabReservationRepository reservationRepository;
    private final EquipmentSlotRepository slotRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReservationService(LabReservationRepository reservationRepository,
                              EquipmentSlotRepository slotRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.slotRepository = slotRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ReservationResponse reserve(ReservationRequest req, User student) {
        EquipmentSlot slot = slotRepository.findByIdForUpdate(req.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id: " + req.slotId()));

        if (slot.getStatus() == SlotStatus.CANCELLED) {
            throw new BadRequestException("El turno fue cancelado");
        }
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("El turno ya inicio o ya paso");
        }
        if (slot.getLaboratory().getStatus() != LaboratoryStatus.ACTIVE) {
            throw new BadRequestException("El laboratorio no esta activo");
        }
        if (reservationRepository.existsBySlotIdAndStudentIdAndStatus(
                slot.getId(), student.getId(), ReservationStatus.CONFIRMED)) {
            throw new ConflictException("Ya tienes una reserva en este turno");
        }

        long taken = reservationRepository.countBySlotIdAndStatus(slot.getId(), ReservationStatus.CONFIRMED);
        if (slot.getStatus() == SlotStatus.FULL || taken >= slot.getCapacity()) {
            throw new ConflictException("El turno no tiene cupos disponibles");
        }

        LabReservation saved = reservationRepository.save(
                new LabReservation(slot, student, req.purpose().trim()));

        if (taken + 1 >= slot.getCapacity()) {
            slot.setStatus(SlotStatus.FULL);
        }

        eventPublisher.publishEvent(new ReservationCreatedEvent(
                saved.getId(), student.getEmail(), slot.getLaboratory().getName(),
                slot.getEquipment(), slot.getStartTime()));

        return ReservationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> findMine(User student, Pageable pageable) {
        return reservationRepository.findByStudentId(student.getId(), pageable).map(ReservationResponse::from);
    }

    // El dueno puede cancelar su reserva; TECHNICIAN y ADMIN pueden cancelar cualquiera
    @Transactional
    public ReservationResponse cancel(Long id, User current) {
        LabReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con id: " + id));

        boolean isOwner = reservation.getStudent().getId().equals(current.getId());
        if (!isOwner && current.getRole() == Role.STUDENT) {
            throw new ForbiddenException("No puedes cancelar la reserva de otro estudiante");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("La reserva ya esta cancelada");
        }

        EquipmentSlot slot = reservation.getSlot();
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("No se puede cancelar un turno que ya inicio");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        if (slot.getStatus() == SlotStatus.FULL) {
            slot.setStatus(SlotStatus.AVAILABLE);   // se libera un cupo
        }
        return ReservationResponse.from(reservation);
    }
}
