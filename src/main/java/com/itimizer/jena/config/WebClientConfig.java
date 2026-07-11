package com.itimizer.jena.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Optional;

/**
 * Builds the qualified {@link WebClient}s for Jira and the notification channels, wiring in auth
 * (PAT/basic), the response buffer limit, optional proxy/custom-CA TLS and the masking logging
 * filter. Channel clients are conditional on their configuration being present.
 */
@Slf4j
@Configuration
@ConfigurationPropertiesScan
@AllArgsConstructor
public class WebClientConfig {

    private final ApplicationProperties applicationProperties;
    private final ResourceLoader resourceLoader;

    @Bean
    @Qualifier("jiraWebClient")
    public WebClient jiraWebClient(ClientHttpConnector clientHttpConnector) {
        return WebClient.builder()
                .baseUrl(applicationProperties.getJira().getUrl().replaceAll("/+$", ""))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(applicationProperties.getJira().getPat() != null
                        ? header -> header.setBearerAuth(applicationProperties.getJira().getPat())
                        : applicationProperties.getJira().getUsername() != null
                            && applicationProperties.getJira().getPassword() != null
                            ? header ->
                                header.setBasicAuth(applicationProperties.getJira().getUsername(),
                                        applicationProperties.getJira().getPassword())
                            : ignored -> {
                    })
                .filter(WebClientLoggingFilter.create())
                .clientConnector(clientHttpConnector)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs
                                .defaultCodecs()
                                .maxInMemorySize((int) applicationProperties.getJira()
                                        .getMaxInMemorySize().toBytes()))
                        .build())
                .build();
    }

    @Bean
    @Qualifier("telegramWebClient")
    @ConditionalOnProperty(prefix = "jena.notification.telegram", name = "token")
    public WebClient telegramWebClient(ClientHttpConnector clientHttpConnector) {
        return WebClient.builder()
                .baseUrl("https://api.telegram.org/bot"
                        + applicationProperties.getNotification().getTelegram().getToken()
                        + "/sendMessage")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(WebClientLoggingFilter.create())
                .clientConnector(clientHttpConnector)
                .build();
    }

    @Bean
    @Qualifier("expressWebClient")
    @ConditionalOnProperty(prefix = "jena.notification.express", name = "url")
    public WebClient expressWebClient(ClientHttpConnector clientHttpConnector) {
        return WebClient.builder()
                .baseUrl(applicationProperties.getNotification().getExpress().getUrl()
                        .replaceAll("/+$", "")
                        + "/api/v4/botx/notifications/direct/sync")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(WebClientLoggingFilter.create())
                .clientConnector(clientHttpConnector)
                .build();
    }

    @Bean(destroyMethod = "dispose")
    public ConnectionProvider connectionProvider() {
        var pool = applicationProperties.getHttp().getPool();
        return ConnectionProvider.builder("jena")
                .maxConnections(pool.getMaxConnections())
                .pendingAcquireTimeout(pool.getPendingAcquireTimeout())
                .maxIdleTime(pool.getMaxIdleTime())
                .maxLifeTime(pool.getMaxLifeTime())
                .evictInBackground(pool.getEvictInBackground())
                .build();
    }

    @Bean
    public HttpClient httpClient(ConnectionProvider connectionProvider) {
        var httpClient = HttpClient.create(connectionProvider)
                .wiretap(applicationProperties.getHttp().isWiretap())
                .responseTimeout(applicationProperties.getHttp().getResponseTimeout())
                .option(ChannelOption.SO_KEEPALIVE,
                        applicationProperties.getHttp().isKeepAlive());

        var proxy = applicationProperties.getProxy();
        if (proxy != null && proxy.getEnabled()) {
            httpClient = httpClient.proxy(spec -> {
                var builder = spec.type(ProxyProvider.Proxy.HTTP)
                        .host(proxy.getHost())
                        .port(proxy.getPort())
                        .nonProxyHosts(proxy.getNonProxyHosts());
                if (proxy.getConnectionTimeout() != null) {
                    builder.connectTimeoutMillis(proxy.getConnectionTimeout().toMillis());
                }
            });
        }

        var sslContext = buildSslContext();
        if (sslContext.isPresent()) {
            httpClient = httpClient.secure(spec -> spec.sslContext(sslContext.get()));
        }

        return httpClient;
    }

    @Bean
    public ClientHttpConnector clientHttpConnector(HttpClient httpClient) {
        return new ReactorClientHttpConnector(httpClient);
    }

    private Optional<SslContext> buildSslContext() {
        if (applicationProperties.getTls() == null
                || applicationProperties.getTls().getCacert() == null) {
            return Optional.empty();
        }

        SslContext sslContext = null;

        try {
            var resource =
                    resourceLoader.getResource(applicationProperties.getTls().getCacert());
            var cf = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> certificates;

            try (var is = resource.getInputStream()) {
                certificates = cf.generateCertificates(is);
            }
            if (certificates.isEmpty()) {
                log.warn("Expected non-empty set of trusted certificates from file {}",
                        applicationProperties.getTls().getCacert());
                return Optional.empty();
            }

            var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            var index = 0;

            for (var certificate : certificates) {
                var certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificate);
                log.debug("Added trusted certificate {}: {}",
                        certificateAlias, certificate.toString());
            }

            var trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            sslContext = SslContextBuilder.forClient()
                    .trustManager(trustManagerFactory)
                    .build();
        } catch (Exception e) {
            log.error("Error occurred while create SSLContext from file {}: {}",
                    applicationProperties.getTls().getCacert(), e.getMessage(), e);
        }

        return Optional.ofNullable(sslContext);
    }

}
