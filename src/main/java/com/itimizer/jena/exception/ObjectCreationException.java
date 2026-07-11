package com.itimizer.jena.exception;

/** Thrown when an entity cannot be created. */
public class ObjectCreationException extends RuntimeException {
    public ObjectCreationException(String message) {
        super(message);
    }
}
