package com.itimizer.jena.exception;

/** Thrown when a referenced entity does not exist; maps to HTTP 404. */
public class ObjectNotFoundException extends RuntimeException {
    public ObjectNotFoundException(String message) {
        super(message);
    }
}
