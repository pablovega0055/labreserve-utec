package com.utec.dbp.repository;

import com.utec.dbp.model.Laboratory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {

    boolean existsByNameIgnoreCase(String name);

    // Trae el manager en la misma consulta (evita N+1)
    @EntityGraph(attributePaths = "manager")
    Page<Laboratory> findAll(Pageable pageable);
}
