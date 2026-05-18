package com.worknest.features.dashboard.application;

import com.worknest.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

public record DashboardDateRange(LocalDate startDate, LocalDate endDate) {

    private static final Pattern ISO_LOCAL_DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    public DashboardDateRange {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        if (startDate.isAfter(endDate)) {
            throw invalidRange();
        }
    }

    public static Optional<DashboardDateRange> parseOptional(String startDate, String endDate) {
        String normalizedStartDate = normalize(startDate);
        String normalizedEndDate = normalize(endDate);

        if (normalizedStartDate == null && normalizedEndDate == null) {
            return Optional.empty();
        }

        if (normalizedStartDate == null || normalizedEndDate == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE",
                    "Both startDate and endDate are required when filtering by a custom date range."
            );
        }

        return Optional.of(new DashboardDateRange(
                parseDate("startDate", normalizedStartDate),
                parseDate("endDate", normalizedEndDate)
        ));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static LocalDate parseDate(String fieldName, String value) {
        if (!ISO_LOCAL_DATE_PATTERN.matcher(value).matches()) {
            throw invalidFormat(fieldName);
        }

        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw invalidFormat(fieldName);
        }
    }

    private static BusinessException invalidFormat(String fieldName) {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_DATE_FORMAT",
                fieldName + " must be in yyyy-MM-dd format."
        );
    }

    private static BusinessException invalidRange() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_DATE_RANGE",
                "startDate must be before or equal to endDate."
        );
    }
}
