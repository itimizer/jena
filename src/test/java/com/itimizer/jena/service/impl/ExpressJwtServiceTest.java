package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.exception.ConfigurationException;
import com.itimizer.jena.util.StringUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpressJwtService Tests")
class ExpressJwtServiceTest {

    private static final String BOT_ID = "test-bot-id";
    private static final String SECRET_KEY = "this-is-a-very-long-secret-key-for-hmac-sha256";
    private static final String EXPRESS_URL = "https://express.example.com/api";
    private static final String HOST_URL = "express.example.com";

    @Mock
    private StringUtil stringUtil;

    private ApplicationProperties applicationProperties;
    private ApplicationProperties.Notification.Express express;
    private ExpressJwtService expressJwtService;

    @BeforeEach
    void setUp() {
        applicationProperties = new ApplicationProperties();
        express = applicationProperties.getNotification().getExpress();
        expressJwtService = new ExpressJwtService(applicationProperties, stringUtil);
    }

    @Test
    @DisplayName("should generate a valid signed token with expected claims")
    void should_generate_valid_token() {
        express.setBotId(BOT_ID);
        express.setSecretKey(SECRET_KEY);
        express.setUrl(EXPRESS_URL);
        when(stringUtil.getHostUrl(EXPRESS_URL)).thenReturn(HOST_URL);

        long before = Instant.now().getEpochSecond();
        String token = expressJwtService.generateToken();
        long after = Instant.now().getEpochSecond();

        assertThat(token).isNotBlank();

        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        Claims claims = parsed.getPayload();

        assertThat(claims.getIssuer()).isEqualTo(BOT_ID);
        assertThat(claims.getAudience()).containsExactly(HOST_URL);
        assertThat(claims.get("version", Integer.class)).isEqualTo(2);
        assertThat(claims.getId()).isNotBlank();

        long iat = claims.getIssuedAt().toInstant().getEpochSecond();
        long nbf = claims.getNotBefore().toInstant().getEpochSecond();
        long exp = claims.getExpiration().toInstant().getEpochSecond();
        assertThat(iat).isBetween(before, after);
        assertThat(nbf).isEqualTo(iat);
        assertThat(exp).isEqualTo(iat + 60);
    }

    @Test
    @DisplayName("should set audience to the real host parsed from the Express URL")
    void should_set_audience_to_real_host_of_express_url() {
        ExpressJwtService service = new ExpressJwtService(applicationProperties, new StringUtil());
        express.setBotId(BOT_ID);
        express.setSecretKey(SECRET_KEY);
        express.setUrl(EXPRESS_URL);

        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Claims claims = parseClaims(service.generateToken(), key);

        assertThat(claims.getAudience()).containsExactly(HOST_URL);
    }

    @Test
    @DisplayName("should generate a unique jti for each token")
    void should_generate_unique_jti() {
        express.setBotId(BOT_ID);
        express.setSecretKey(SECRET_KEY);
        express.setUrl(EXPRESS_URL);
        when(stringUtil.getHostUrl(EXPRESS_URL)).thenReturn(HOST_URL);

        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        String jti1 = parseClaims(expressJwtService.generateToken(), key).getId();
        String jti2 = parseClaims(expressJwtService.generateToken(), key).getId();

        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    @DisplayName("should throw exception when Bot ID is null")
    void should_throw_when_bot_id_is_null() {
        express.setBotId(null);

        assertThatThrownBy(() -> expressJwtService.generateToken())
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Bot ID or Secret Key is null");

        verifyNoInteractions(stringUtil);
    }

    @Test
    @DisplayName("should throw exception when Secret Key is null")
    void should_throw_when_secret_key_is_null() {
        express.setBotId(BOT_ID);
        express.setSecretKey(null);

        assertThatThrownBy(() -> expressJwtService.generateToken())
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Bot ID or Secret Key is null");

        verifyNoInteractions(stringUtil);
    }

    @Test
    @DisplayName("should fail startup validation when secret key is too short for HS256")
    void should_fail_startup_validation_when_secret_key_is_too_short() {
        express.setSecretKey("too-short");

        assertThatThrownBy(() -> expressJwtService.validateSecretKey())
                .isInstanceOf(WeakKeyException.class);
    }

    @Test
    @DisplayName("should pass startup validation when secret key is absent")
    void should_pass_startup_validation_when_secret_key_is_absent() {
        express.setSecretKey(null);

        assertThatCode(() -> expressJwtService.validateSecretKey())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should pass startup validation when secret key is blank")
    void should_pass_startup_validation_when_secret_key_is_blank() {
        express.setSecretKey("");

        assertThatCode(() -> expressJwtService.validateSecretKey())
                .doesNotThrowAnyException();
    }

    private Claims parseClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
