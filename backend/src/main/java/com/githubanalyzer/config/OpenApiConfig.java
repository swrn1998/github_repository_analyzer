package com.githubanalyzer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 / Swagger configuration for the GitHub Repository Analyzer API.
 *
 * <p>This configuration provides interactive API documentation accessible at:
 * <ul>
 *   <li>Swagger UI: {@code /swagger-ui.html} or {@code /swagger-ui/index.html}</li>
 *   <li>OpenAPI JSON: {@code /v3/api-docs}</li>
 *   <li>OpenAPI YAML: {@code /v3/api-docs.yaml}</li>
 * </ul>
 *
 * <p>The Swagger UI allows testing API endpoints directly from the browser.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:GitHub Repository Analyzer}")
    private String appName;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Value("${info.app.description:Analyzes GitHub repositories with health scoring and alerts}")
    private String appDescription;

    @Bean
    public OpenAPI githubAnalyzerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName + " API")
                        .version(appVersion)
                        .description(appDescription + """
                                
                                ## Features
                                - Repository statistics (stars, forks, watchers, issues)
                                - Health score calculation (0-100 with A-F grade)
                                - Contributor activity and commit patterns
                                - Language distribution analysis
                                - Conditional alerts (inactive repo, high issues, no tests, vulnerabilities)
                                - Offline mode support via X-Offline-Mode header
                                
                                ## Data Sources
                                Responses include a `source` field indicating data freshness:
                                - `LIVE` — freshly fetched from GitHub API
                                - `CACHE` — from in-memory cache (within TTL)
                                - `STALE` — expired cache entry (GitHub API unavailable)
                                - `MOCK` — static demo data (offline mode, no cache)
                                
                                ## Offline Mode
                                Set the `X-Offline-Mode: true` request header to receive cached or mock data 
                                without calling the GitHub API.
                                """)
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com")
                                .url("https://github.com/your-org/github-repository-analyzer"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development"),
                        new Server()
                                .url("https://github-analyzer-api-niksoni.azurewebsites.net")
                                .description("Production (Azure)")
                ));
    }
}
