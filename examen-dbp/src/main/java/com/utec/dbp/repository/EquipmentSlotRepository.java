package com.utec.dbp.repository;

import com.utec.dbp.model.EquipmentSlot;
import com.utec.dbp.model.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EquipmentSlotRepository extends JpaRepository<EquipmentSlot, Long> {

    // @EntityGraph trae el laboratorio en la misma consulta (evita N+1)
    @EntityGraph(attributePaths = "laboratory")
    Page<EquipmentSlot> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "laboratory")
    Page<EquipmentSlot> findByLaboratoryId(Long laboratoryId, Pageable pageable);

    @EntityGraph(attributePaths = "laboratory")
    Page<EquipmentSlot> findByStatus(SlotStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "laboratory")
    Page<EquipmentSlot> findByLaboratoryIdAndStatus(Long laboratoryId, SlotStatus status, Pageable pageable);

    // Bloquea la fila del turno mientras se reserva (evita sobrepasar capacity con reservas simultaneas)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM EquipmentSlot s JOIN FETCH s.laboratory WHERE s.id = :id")
    Optional<EquipmentSlot> findByIdForUpdate(@Param("id") Long id);

    // Hay cruce de horario si: existente.start < nuevo.end  Y  existente.end > nuevo.start
    boolean existsByLaboratoryIdAndEquipmentIgnoreCaseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Long laboratoryId, String equipment, SlotStatus status, LocalDateTime end, LocalDateTime start);
}
