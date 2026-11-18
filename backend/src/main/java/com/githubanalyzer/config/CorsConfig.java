package com.githubanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// SOLID - Single Responsibility: only configures CORS for the API
// 12-Factor III (Config): allowed origins come from environment variable, not hardcoded
// This means dev (localhost:3000) and prod (Azure) work with the same jar, different config
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000,https://github-analyzer-ui-niksoni.azurewebsites.net}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")           // apply CORS only to API routes
                .allowedOrigins(allowedOrigins.split(","))  // split CSV from config
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Cache-Status") // browser can read this response header
                .maxAge(3600);  // browser caches preflight response for 1 hour
    }
}
