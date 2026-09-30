package com.utec.dbp.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// Formato de paginacion simplificado: { content, page, size, totalElements }
public record PagedResponse<T>(List<T> content, int page, int size, long totalElements) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
