package com.itimizer.jena.exception;

/** Thrown when a required configuration value is missing or invalid. */
public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message) {
        super(message);
    }
}
