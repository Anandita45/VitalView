package com.screening.backend.member3.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Python AI worker boundary.
 */
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiWorkerProperties {

    /**
     * Base URL of the external Python AI worker service.
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * Optional secret or API key for communicating with the AI worker.
     */
    private String apiKey;

    /**
     * Request timeout in milliseconds.
     */
    private int timeoutMs = 5000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
