package com.itimizer.jena.service;

/**
 * Issues the signed JWT used to authenticate outbound requests to notification channels.
 */
public interface JwtService {

    /** A freshly signed bearer token. */
    String generateToken();
}
