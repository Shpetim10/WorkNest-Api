package com.worknest.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import com.worknest.common.api.ApiErrorResponse;
import com.worknest.common.api.FieldValidationError;
import com.worknest.features.company.exception.CompanySiteActivationBlockedException;
import com.worknest.features.companySite.exception.InvalidCidrException;
import com.worknest.features.companySite.exception.InvalidGeofenceException;
import com.worknest.features.companySite.exception.SiteCreationValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldValidationError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldValidationError)
                .toList();

        return ResponseEntity.badRequest().body(
                ApiErrorResponse.of(
                        "VALIDATION_ERROR",
                        "Some submitted fields are missing or invalid. Fix the highlighted fields and try again.",
                        request.getRequestURI(),
                        fieldErrors
                )
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<FieldValidationError> fieldErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldValidationError(violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(
                ApiErrorResponse.of(
                        "VALIDATION_ERROR",
                        "Some submitted fields are missing or invalid. Fix the highlighted fields and try again.",
                        request.getRequestURI(),
                        fieldErrors
                )
        );
    }

    @ExceptionHandler(SiteCreationValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleSiteCreationValidation(
            SiteCreationValidationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.getStatus()).body(
                ApiErrorResponse.of(exception.getCode(), exception.getMessage(), request.getRequestURI(), exception.getFieldErrors())
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.getStatus()).body(
                ApiErrorResponse.of(exception.getCode(), exception.getMessage(), request.getRequestURI(), List.of())
        );
    }

    /**
     * JPA optimistic-lock failure — surfaced when two admins edit the same site
     * draft simultaneously and one "wins" the DB write.
     *
     * <p>The message is deliberately generic to prevent leaking entity type or ID
     * details across tenant boundaries. The client-facing message matches the one
     * documented in the plan: "Updated in another tab — please refresh."
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiErrorResponse.of(
                        "OPTIMISTIC_LOCK_CONFLICT",
                        "Updated in another tab \u2014 please refresh and try again.",
                        request.getRequestURI(),
                        List.of()
                )
        );
    }

    @ExceptionHandler(CompanySiteActivationBlockedException.class)
    public ResponseEntity<ApiErrorResponse> handleCompanySiteActivationBlocked(
            CompanySiteActivationBlockedException exception,
            HttpServletRequest request
    ) {
        List<FieldValidationError> fieldErrors = exception.getIssues()
                .stream()
                .map(issue -> new FieldValidationError(issue.field(), issue.message()))
                .toList();

        return ResponseEntity.status(exception.getStatus()).body(
                ApiErrorResponse.of(exception.getCode(), exception.getMessage(), request.getRequestURI(), fieldErrors)
        );
    }

    @ExceptionHandler(InvalidGeofenceException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidGeofence(
            InvalidGeofenceException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.getStatus()).body(
                ApiErrorResponse.of(exception.getCode(), exception.getMessage(), request.getRequestURI(), List.of())
        );
    }

    @ExceptionHandler(InvalidCidrException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCidr(
            InvalidCidrException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.getStatus()).body(
                ApiErrorResponse.of(exception.getCode(), exception.getMessage(), request.getRequestURI(), List.of())
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiErrorResponse.of("BAD_CREDENTIALS", "Email or password is incorrect", request.getRequestURI(), List.of())
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiErrorResponse.of("ACCESS_DENIED", "You do not have permission to access this resource", request.getRequestURI(), List.of())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.error("Database constraint violation in {}: {}", request.getRequestURI(), exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiErrorResponse.of(
                        "DATABASE_CONFLICT",
                        "The operation could not be completed due to a data conflict (e.g. duplicate site code or overlapping CIDR).",
                        request.getRequestURI(),
                        List.of()
                )
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        log.error("Illegal state in {}: {}", request.getRequestURI(), exception.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiErrorResponse.of("INTERNAL_ERROR", exception.getMessage(), request.getRequestURI(), List.of())
        );
    }

    /**
     * Catches exceptions thrown during the JPA/Hibernate commit/flush phase.
     * This often wraps ConstraintViolationExceptions that weren't caught
     * earlier by the web-layer validation.
     */
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiErrorResponse> handleTransactionSystem(
            TransactionSystemException exception,
            HttpServletRequest request
    ) {
        Throwable cause = exception.getRootCause();
        if (cause instanceof jakarta.validation.ConstraintViolationException cve) {
            return handleConstraintViolation(cve, request);
        }

        log.error("Transaction failure in {}: {}", request.getRequestURI(), exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiErrorResponse.of("TRANSACTION_ERROR", "The operation failed during database commit.", request.getRequestURI(), List.of())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandled(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception in {}: {}", request.getRequestURI(), exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred", request.getRequestURI(), List.of())
        );
    }

    private FieldValidationError toFieldValidationError(FieldError error) {
        return new FieldValidationError(error.getField(), error.getDefaultMessage());
    }
}
