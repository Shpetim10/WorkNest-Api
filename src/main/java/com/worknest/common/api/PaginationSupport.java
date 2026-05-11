package com.worknest.common.api;

import com.worknest.common.exception.BusinessException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

public final class PaginationSupport {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private PaginationSupport() {
    }

    public static Pageable pageable(Integer page, Integer size) {
        return pageable(page, size, Sort.unsorted());
    }

    public static Pageable pageable(Integer page, Integer size, Sort sort) {
        int resolvedPage = page == null ? DEFAULT_PAGE : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;

        if (resolvedPage < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                    "Page must be greater than or equal to 0.");
        }
        if (resolvedSize < 1 || resolvedSize > MAX_SIZE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_SIZE",
                    "Page size must be between 1 and " + MAX_SIZE + ".");
        }

        return PageRequest.of(resolvedPage, resolvedSize, sort);
    }

    public static <T> Page<T> page(List<T> items, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(items);
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<T> pageItems = start >= items.size() ? List.of() : items.subList(start, end);
        return new PageImpl<>(pageItems, pageable, items.size());
    }
}
