package com.githubanalyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the GitHub Repository Analyzer microservice.
 *
 * <p>Architecture: Option A — Resilience & Offline Mode
 * <ul>
 *   <li>In-memory TTL cache with stale fallback</li>
 *   <li>Mock data provider for full offline operation</li>
 *   <li>Explicit degradation strategy: LIVE → CACHE → STALE → MOCK</li>
 * </ul>
 *
 * <p>Port: 8080 (configured in application.yml)
 */
@SpringBootApplication
@EnableScheduling   // Required for cache eviction scheduler
public class GithubAnalyzerApplication {

    private static final Logger log = LoggerFactory.getLogger(GithubAnalyzerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GithubAnalyzerApplication.class, args);
    }

    /**
     * Logs a startup summary once the application context is fully initialized.
     * This runs after all beans are wired and ready — safe to log config state.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=======================================================");
        log.info("  GitHub Repository Analyzer started successfully");
        log.info("  API:     http://localhost:8080/api/analyze");
        log.info("  Swagger: http://localhost:8080/swagger-ui.html");
        log.info("  Health:  http://localhost:8080/actuator/health");
        log.info("=======================================================");
    }
}
