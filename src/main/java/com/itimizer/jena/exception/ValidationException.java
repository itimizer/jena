package com.itimizer.jena.exception;

/** Thrown when input fails a business rule (e.g. invalid JQL, mode invariant); maps to HTTP 400. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
