package com.utec.dbp.service;

import com.utec.dbp.dto.LaboratoryRequest;
import com.utec.dbp.dto.LaboratoryResponse;
import com.utec.dbp.exception.BadRequestException;
import com.utec.dbp.exception.ConflictException;
import com.utec.dbp.exception.ResourceNotFoundException;
import com.utec.dbp.model.Laboratory;
import com.utec.dbp.model.LaboratoryStatus;
import com.utec.dbp.model.Role;
import com.utec.dbp.model.User;
import com.utec.dbp.repository.LaboratoryRepository;
import com.utec.dbp.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LaboratoryService {

    private final LaboratoryRepository laboratoryRepository;
    private final UserRepository userRepository;

    public LaboratoryService(LaboratoryRepository laboratoryRepository, UserRepository userRepository) {
        this.laboratoryRepository = laboratoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LaboratoryResponse create(LaboratoryRequest req) {
        String name = req.name().trim();
        if (laboratoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe un laboratorio con nombre: " + name);
        }

        User manager = userRepository.findById(req.managerId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + req.managerId()));
        if (manager.getRole() == Role.STUDENT) {
            throw new BadRequestException("El manager debe tener rol TECHNICIAN o ADMIN");
        }

        LaboratoryStatus status = req.status() != null ? req.status() : LaboratoryStatus.ACTIVE;
        Laboratory lab = new Laboratory(name, req.location().trim(), manager, status);
        return LaboratoryResponse.from(laboratoryRepository.save(lab));
    }

    @Transactional(readOnly = true)
    public Page<LaboratoryResponse> findAll(Pageable pageable) {
        return laboratoryRepository.findAll(pageable).map(LaboratoryResponse::from);
    }

    @Transactional(readOnly = true)
    public LaboratoryResponse findById(Long id) {
        return LaboratoryResponse.from(getEntity(id));
    }

    public Laboratory getEntity(Long id) {
        return laboratoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratorio no encontrado con id: " + id));
    }
}
