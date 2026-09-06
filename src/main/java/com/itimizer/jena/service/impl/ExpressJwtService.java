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
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * {@link JwtService} for the Express channel: mints an HS256 token signed with the configured bot
 * secret, living for {@code jena.notification.express.token-ttl} and backdated by
 * {@link #CLOCK_SKEW} so small clock differences do not make it not-yet-valid. The secret is
 * validated once at startup so a bad key fails fast.
 */
@Slf4j
@Service
@Qualifier("expressJwtService")
public class ExpressJwtService implements JwtService {

    private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

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
        var issuedAt = now.minus(CLOCK_SKEW);
        var expiration = now.plus(express.getTokenTtl());
        var key = Keys.hmacShaKeyFor(express.getSecretKey().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claim("aud", stringUtil.getHostUrl(express.getUrl()))
                .issuer(express.getBotId())
                .issuedAt(Date.from(issuedAt))
                .notBefore(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .id(UUID.randomUUID().toString())
                .claim("version", 2)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}