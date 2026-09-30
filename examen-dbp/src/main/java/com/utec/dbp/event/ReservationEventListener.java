package com.utec.dbp.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventListener.class);

    // Corre en otro hilo y SOLO si la reserva se guardo en BD (AFTER_COMMIT)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationCreated(ReservationCreatedEvent event) {
        // Simulacion del envio de email de confirmacion
        log.info("[EMAIL][{}] Reserva #{} confirmada para {}: {} - {} el {}",
                Thread.currentThread().getName(),
                event.reservationId(), event.studentEmail(),
                event.laboratoryName(), event.equipmentCode(), event.startTime());
    }
}
