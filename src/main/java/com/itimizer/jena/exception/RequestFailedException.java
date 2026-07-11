package com.itimizer.jena.exception;

/** Thrown when an outbound request (e.g. to Jira or a channel) fails. */
public class RequestFailedException extends RuntimeException {
    public RequestFailedException(String message) {
        super(message);
    }
}
