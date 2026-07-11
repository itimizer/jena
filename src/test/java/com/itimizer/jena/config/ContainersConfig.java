package com.itimizer.jena.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Value("${db.postgresql.version:17}")
    private String postgresVersion;

    @Value("${db.postgresql.dbname}")
    private String dbName;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${db.postgresql.port}")
    private String port;

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres")
                .withTag(postgresVersion))
                .withDatabaseName(dbName)
                .withUsername(username)
                .withPassword(password)
                .withExposedPorts(Integer.valueOf(port))
                .withReuse(false);
    }
}

