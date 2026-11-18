package com.githubanalyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

// @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan combined
// Auto-configures Spring beans, scans all @Component/@Service/@Repository in this package
@SpringBootApplication

// Enables @Scheduled methods across the app (used in InMemoryCacheService for TTL eviction)
// Without this, @Scheduled annotations are silently ignored
@EnableScheduling
public class GithubAnalyzerApplication {

    private static final Logger log = LoggerFactory.getLogger(GithubAnalyzerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GithubAnalyzerApplication.class, args);
    }

    // Fires after all beans are wired and the app is fully ready to accept requests
    // Safer than @PostConstruct which runs before the whole context is up
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
