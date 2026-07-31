package com.eventflow.common.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for EventFlow specific settings.
 * Mapped from the "eventflow.*" prefix in application.yml.
 */
@ConfigurationProperties(prefix = "eventflow")
public class EventFlowProperties {

    private Retry retry = new Retry();
    private Dlq dlq = new Dlq();
    private RateLimit rateLimit = new RateLimit();
    private Security security = new Security();

    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
    
    public Dlq getDlq() { return dlq; }
    public void setDlq(Dlq dlq) { this.dlq = dlq; }
    
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
    
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public static class Retry {
        private int maxAttempts = 5;
        private long baseDelayMs = 60000;
        private int multiplier = 5;
        private double jitterPercentage = 0.20;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getBaseDelayMs() { return baseDelayMs; }
        public void setBaseDelayMs(long baseDelayMs) { this.baseDelayMs = baseDelayMs; }
        public int getMultiplier() { return multiplier; }
        public void setMultiplier(int multiplier) { this.multiplier = multiplier; }
        public double getJitterPercentage() { return jitterPercentage; }
        public void setJitterPercentage(double jitterPercentage) { this.jitterPercentage = jitterPercentage; }
    }

    public static class Dlq {
        private int retentionDays = 30;

        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }

    public static class RateLimit {
        private int defaultWindowSeconds = 60;
        private int defaultMaxRequests = 1000;

        public int getDefaultWindowSeconds() { return defaultWindowSeconds; }
        public void setDefaultWindowSeconds(int defaultWindowSeconds) { this.defaultWindowSeconds = defaultWindowSeconds; }
        public int getDefaultMaxRequests() { return defaultMaxRequests; }
        public void setDefaultMaxRequests(int defaultMaxRequests) { this.defaultMaxRequests = defaultMaxRequests; }
    }

    public static class Security {
        private String apiKeyHeader = "Authorization";
        private Jwt jwt = new Jwt();

        public String getApiKeyHeader() { return apiKeyHeader; }
        public void setApiKeyHeader(String apiKeyHeader) { this.apiKeyHeader = apiKeyHeader; }
        public Jwt getJwt() { return jwt; }
        public void setJwt(Jwt jwt) { this.jwt = jwt; }

        public static class Jwt {
            private String jwksUri = "http://localhost:8080/.well-known/jwks.json";

            public String getJwksUri() { return jwksUri; }
            public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }
        }
    }
}