package com.itimizer.jena.exception;

import tools.jackson.core.JacksonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Global REST exception handler. Maps JENA's domain exceptions and common Spring/data exceptions to
 * consistent {@link ErrorResponse} bodies with the right HTTP status (e.g. not-found → 404,
 * validation → 400, data-integrity → 409).
 */
@Slf4j
@ControllerAdvice
public class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> objectNotFoundException(ObjectNotFoundException ex) {
        log.warn("Object not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Object Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ObjectCreationException.class)
    public ResponseEntity<ErrorResponse> objectCreationException(ObjectCreationException ex) {
        log.error("Object not created: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Object Not Created", ex.getMessage()));
    }

    @ExceptionHandler(ObjectUpdatingException.class)
    public ResponseEntity<ErrorResponse> objectUpdatingException(ObjectUpdatingException ex) {
        log.error("Object not updated: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Object Not Updated", ex.getMessage()));
    }

    @ExceptionHandler(RequestFailedException.class)
    public ResponseEntity<ErrorResponse> requestProcessingException(RequestFailedException ex) {
        log.error("Request failed: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Request Failed", ex.getMessage()));
    }

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ErrorResponse> configurationException(ConfigurationException ex) {
        log.error("Configuration error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Configuration Error", ex.getMessage()));
    }

    @ExceptionHandler(JacksonException.class)
    public ResponseEntity<ErrorResponse> jsonProcessingException(JacksonException ex) {
        log.warn("Json processing failed: {}", ex.getOriginalMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Json Processing Failed", ex.getOriginalMessage()));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorResponse> numberFormatExceptionException(NumberFormatException ex) {
        log.warn("Number parsing failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Number Parsing Failed", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgumentException(IllegalArgumentException ex) {
        log.warn("Argument parsing failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Argument Parsing Failed", ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> validationException(ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Validation Failed", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> dataIntegrityViolationException(
            DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Conflict",
                        "The operation conflicts with existing data (the resource "
                                + "is referenced by other records or is a duplicate)."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal Server Error", "An unexpected error occurred."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        log.warn("Validation failed: {}", errors);
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            @NonNull TypeMismatchException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        var requiredType = ex.getRequiredType();
        String message;
        if (requiredType != null && requiredType.isEnum()) {
            var name = requiredType.getSimpleName().toLowerCase();
            message = "Invalid %s specified: %s. Available %ss: %s".formatted(
                    name, ex.getValue(), name,
                    Arrays.toString(requiredType.getEnumConstants()));
        } else {
            message = "Invalid value '%s' provided.".formatted(ex.getValue());
        }
        log.warn(message);
        return new ResponseEntity<>(
                new ErrorResponse("Invalid Argument", message), HttpStatus.BAD_REQUEST);
    }

}