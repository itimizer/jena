package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.exception.ConfigurationException;
import com.itimizer.jena.service.JwtService;
import com.itimizer.jena.util.StringUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * {@link JwtService} for the Express channel: mints a short-lived (60s) HS256 token signed with the
 * configured bot secret. The secret is validated once at startup so a bad key fails fast.
 */
@Slf4j
@Service
@Qualifier("expressJwtService")
public class ExpressJwtService implements JwtService {

    private final ApplicationProperties.Notification.Express express;
    private final StringUtil stringUtil;

    public ExpressJwtService(ApplicationProperties applicationProperties, StringUtil stringUtil) {
        this.express = applicationProperties.getNotification().getExpress();
        this.stringUtil = stringUtil;
    }

    @PostConstruct
    void validateSecretKey() {
        if (express.getSecretKey() != null && !express.getSecretKey().isBlank()) {
            Keys.hmacShaKeyFor(express.getSecretKey().getBytes(StandardCharsets.UTF_8));
        }
    }

    @SuppressWarnings("checkstyle:CommentsIndentation")
    @Override
    public String generateToken() {
        if (express.getBotId() == null || express.getSecretKey() == null) {
            log.warn("Bot ID or Secret Key is null");
            throw new ConfigurationException("Bot ID or Secret Key is null");
        }

        log.debug("Generating token for Bot ID: {}", express.getBotId());
        var now = Instant.now();
        var expiration = now.plusSeconds(60);
        var key = Keys.hmacShaKeyFor(express.getSecretKey().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claim("aud", stringUtil.getHostUrl(express.getUrl()))
                .issuer(express.getBotId())
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(expiration))
                .id(UUID.randomUUID().toString())
                .claim("version", 2)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}