package com.itimizer.jena.exception;

/** Thrown when an entity cannot be updated. */
public class ObjectUpdatingException extends RuntimeException {
    public ObjectUpdatingException(String message) {
        super(message);
    }
}
