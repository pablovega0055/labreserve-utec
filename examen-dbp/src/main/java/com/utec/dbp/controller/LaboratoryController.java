package com.utec.dbp.controller;

import com.utec.dbp.dto.LaboratoryRequest;
import com.utec.dbp.dto.LaboratoryResponse;
import com.utec.dbp.dto.PagedResponse;
import com.utec.dbp.service.LaboratoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/laboratories")
public class LaboratoryController {

    private final LaboratoryService laboratoryService;

    public LaboratoryController(LaboratoryService laboratoryService) {
        this.laboratoryService = laboratoryService;
    }

    // POST /laboratories -> 201 (solo ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<LaboratoryResponse> create(@Valid @RequestBody LaboratoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(laboratoryService.create(request));
    }

    // GET /laboratories?page=0&size=10
    @GetMapping
    public ResponseEntity<PagedResponse<LaboratoryResponse>> getAll(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(laboratoryService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LaboratoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(laboratoryService.findById(id));
    }
}
