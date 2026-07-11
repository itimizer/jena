package com.itimizer.jena.config;

import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the actuator {@code httpexchanges} endpoint by providing an in-memory exchange
 * repository.
 */
@Configuration
public class HttpTraceActuatorConfiguration {

    @Bean
    public InMemoryHttpExchangeRepository createTraceRepository() {
        return new InMemoryHttpExchangeRepository();
    }

}
