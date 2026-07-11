package com.itimizer.jena.exception;

/**
 * Standard error body returned by the API: a short {@code error} label and free-form
 * {@code details}.
 */
public record ErrorResponse(
        String error,
        Object details
) {
}
