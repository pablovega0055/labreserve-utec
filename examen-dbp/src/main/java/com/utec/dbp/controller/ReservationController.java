package com.utec.dbp.controller;

import com.utec.dbp.dto.PagedResponse;
import com.utec.dbp.dto.ReservationRequest;
import com.utec.dbp.dto.ReservationResponse;
import com.utec.dbp.model.User;
import com.utec.dbp.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // POST /reservations -> 201 (solo STUDENT; el estudiante sale del token)
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReservationRequest request,
                                                       @AuthenticationPrincipal User current) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.reserve(request, current));
    }

    // GET /reservations/me -> mis reservas paginadas
    @GetMapping("/me")
    public ResponseEntity<PagedResponse<ReservationResponse>> mine(
            @AuthenticationPrincipal User current,
            @PageableDefault(page = 0, size = 10, sort = "reservedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(reservationService.findMine(current, pageable)));
    }

    // PATCH /reservations/1/cancel
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id,
                                                      @AuthenticationPrincipal User current) {
        return ResponseEntity.ok(reservationService.cancel(id, current));
    }
}
