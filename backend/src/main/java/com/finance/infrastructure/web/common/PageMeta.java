package com.finance.infrastructure.web.common;

import org.springframework.data.domain.Page;

/** meta shape for any paginated list endpoint (api-specification.md's response envelope). */
public record PageMeta(int page, int size, long totalElements, int totalPages) {

    public static PageMeta from(Page<?> page) {
        return new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
