package com.utec.dbp.repository;

import com.utec.dbp.model.LabReservation;
import com.utec.dbp.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabReservationRepository extends JpaRepository<LabReservation, Long> {

    long countBySlotIdAndStatus(Long slotId, ReservationStatus status);

    boolean existsBySlotIdAndStudentIdAndStatus(Long slotId, Long studentId, ReservationStatus status);

    @EntityGraph(attributePaths = {"slot", "slot.laboratory", "student"})
    Page<LabReservation> findByStudentId(Long studentId, Pageable pageable);
}
