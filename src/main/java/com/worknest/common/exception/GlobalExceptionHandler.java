package com.worknest.common.exception;

import com.worknest.common.api.ApiErrorResponse;
import com.worknest.common.api.FieldValidationError;
import com.worknest.features.company.exception.CompanySiteActivationBlockedException;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
                ApiErrorResponse.of("VALIDATION_ERROR", "Request validation failed", request.getRequestURI(), fieldErrors)
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
                ApiErrorResponse.of("VALIDATION_ERROR", "Request validation failed", request.getRequestURI(), fieldErrors)
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandled(
            Exception exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred", request.getRequestURI(), List.of())
        );
    }

    private FieldValidationError toFieldValidationError(FieldError error) {
        return new FieldValidationError(error.getField(), error.getDefaultMessage());
    }
}
