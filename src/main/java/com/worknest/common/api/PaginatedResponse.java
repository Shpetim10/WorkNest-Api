package com.worknest.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Standard paginated payload wrapper")
public record PaginatedResponse<T>(
        @Schema(description = "Items for the current page")
        List<T> items,

        @Schema(description = "Zero-based current page index", example = "0")
        int currentPage,

        @Schema(description = "Requested page size", example = "20")
        int pageSize,

        @Schema(description = "Total number of matching items", example = "145")
        long totalItems,

        @Schema(description = "Total number of available pages", example = "8")
        int totalPages,

        @Schema(description = "Whether another page exists after the current one", example = "true")
        boolean hasNext,

        @Schema(description = "Whether a page exists before the current one", example = "false")
        boolean hasPrevious
) {

    public static <T> PaginatedResponse<T> from(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
