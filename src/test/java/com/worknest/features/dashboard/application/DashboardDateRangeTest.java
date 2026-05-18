package com.worknest.features.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.worknest.common.exception.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DashboardDateRangeTest {

    @Test
    void parseOptionalReturnsEmptyWhenNoCustomRangeIsProvided() {
        assertThat(DashboardDateRange.parseOptional(null, null)).isEmpty();
        assertThat(DashboardDateRange.parseOptional(" ", "")).isEmpty();
    }

    @Test
    void parseOptionalAcceptsValidInclusiveRange() {
        DashboardDateRange range = DashboardDateRange.parseOptional("2026-05-01", "2026-05-31")
                .orElseThrow();

        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    void invalidDateFormatReturnsBadRequest() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> DashboardDateRange.parseOptional("05/01/2026", "2026-05-31")
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getCode()).isEqualTo("INVALID_DATE_FORMAT");
    }

    @Test
    void startDateAfterEndDateReturnsBadRequest() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> DashboardDateRange.parseOptional("2026-06-01", "2026-05-31")
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getCode()).isEqualTo("INVALID_DATE_RANGE");
    }

    @Test
    void partialCustomRangeReturnsBadRequest() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> DashboardDateRange.parseOptional("2026-05-01", null)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getCode()).isEqualTo("INVALID_DATE_RANGE");
    }
}
