package com.githubanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration to allow the React frontend to call the backend API.
 *
 * <p>In dev: allows localhost:3000 (React dev server).
 * In prod: should be restricted to the actual frontend domain.
 *
 * <p>Configured via {@code cors.allowed-origins} property so it can be
 * adjusted per environment without code changes (12-Factor config).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Cache-Status")
                .maxAge(3600);
    }
}
