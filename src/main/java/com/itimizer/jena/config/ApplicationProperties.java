package com.itimizer.jena.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Typed binding of all {@code jena.*} settings (Jira connection, schedules, channels, retry, proxy,
 * TLS, security). Bean-validated at startup; changing a value requires a restart.
 */
@ConfigurationProperties("jena")
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationProperties {

    private Security security;
    @NotNull
    @Valid
    private Jira jira;
    private Notification notification = new Notification();
    @Valid
    private Retryer retryer = new Retryer();
    @Valid
    private Proxy proxy;
    private Tls tls;
    private Http http = new Http();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Security {

        private Boolean create = false;
        private String username;
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Jira {

        @NotBlank
        private String url;
        @NotNull
        private DeploymentType deploymentType = DeploymentType.SERVER;
        private String pat;
        private String username;
        private String password;
        private List<String> projects;
        private List<String> types;
        private LocalDateTime initialUpdatedAfter;
        private Schedule schedule;
        private Poll poll = new Poll();
        private Snapshot snapshot = new Snapshot();
        private Formatter formatter;
        private Map<String, String> names;
        private DataSize maxInMemorySize = DataSize.ofMegabytes(16);

        @Setter(AccessLevel.NONE)
        private transient volatile Map<String, String> reversedNames;

        public enum DeploymentType {
            SERVER,
            CLOUD
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Schedule {

            private String cron;
            private String zone;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Poll {

            @Min(1)
            private int concurrency = 1;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Snapshot {

            private String defaultZone = "UTC";
            private Scheduler scheduler = new Scheduler();

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Scheduler {

                @Min(1)
                private int poolSize = 1;
            }
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Formatter {
            private String datePattern = "dd.MM.yyyy";
            private String datetimePattern = "dd.MM.yyyy HH:mm";
        }

        public void setNames(Map<String, String> names) {
            this.names = names;
            this.reversedNames = null;
        }

        public Map<String, String> getReversedNames() {
            var cached = reversedNames;

            if (cached == null) {
                cached = names == null ? Map.of() : names.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
                reversedNames = cached;
            }
            return cached;
        }

        @AssertTrue(message = "jena.jira.names must not contain duplicate values")
        public boolean isNamesValuesUnique() {
            return names == null
                    || names.values().stream().distinct().count() == names.size();
        }

        @AssertTrue(message = "Jira Cloud does not accept Personal Access Tokens: it authenticates "
                + "only via basic auth. Set jena.jira.username to the Atlassian account email and "
                + "jena.jira.password to an API token, and leave jena.jira.pat unset")
        public boolean isAuthenticationSupportedByDeployment() {
            return deploymentType != DeploymentType.CLOUD || pat == null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Notification {

        private Schedule schedule;
        private Telegram telegram = new Telegram();
        private Express express = new Express();

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Schedule {

            private String cron;
            private String zone;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Telegram {

            private String token;
            private List<String> chatId;
            private String parseMode;
            private Boolean removeJiraFormatting;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Express {

            private String url;
            private String botId;
            private String secretKey;
            private List<String> chatId;
            @NotNull
            private Duration tokenTtl = Duration.ofMinutes(5);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Retryer {

        private Boolean enabled = false;
        private String cron;
        private String zone;
        @Min(1)
        private Integer attempt;

        @AssertTrue(message = "jena.retryer.attempt must be set and >= 1 when retryer is enabled")
        public boolean isAttemptValidWhenEnabled() {
            return !Boolean.TRUE.equals(enabled) || (attempt != null && attempt >= 1);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Proxy {

        private Boolean enabled = false;
        private String host;
        @Min(1)
        @Max(65535)
        private Integer port;
        private String nonProxyHosts;
        private Duration connectionTimeout;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tls {

        private String cacert;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Http {

        private boolean wiretap = false;
        @NotNull
        private Duration responseTimeout = Duration.ofSeconds(60);
        private boolean keepAlive = true;
        private boolean systemDnsResolver = true;
        @Valid
        private Pool pool = new Pool();

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Pool {

            @Min(1)
            private int maxConnections = 50;
            @NotNull
            private Duration pendingAcquireTimeout = Duration.ofSeconds(30);
            @NotNull
            private Duration maxIdleTime = Duration.ofSeconds(30);
            @NotNull
            private Duration maxLifeTime = Duration.ofMinutes(5);
            @NotNull
            private Duration evictInBackground = Duration.ofSeconds(60);
        }
    }
}
