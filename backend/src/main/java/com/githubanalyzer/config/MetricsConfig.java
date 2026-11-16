package com.githubanalyzer.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Observability configuration for the GitHub Repository Analyzer.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Adds common tags (application, environment) to every metric — so
 *       Grafana Cloud dashboards can filter by app and env.</li>
 *   <li>Schedules a periodic push of Prometheus-format metrics to
 *       Grafana Cloud Mimir (remote write endpoint) when enabled.</li>
 * </ol>
 *
 * <p>Push is controlled by the {@code grafana.remote-write.enabled} flag
 * (defaults to {@code false}) so local/dev runs never attempt network calls
 * to Grafana Cloud.
 *
 * <p>Credentials are injected via environment variables and never hardcoded:
 * <ul>
 *   <li>{@code GRAFANA_REMOTE_WRITE_URL} — Mimir push endpoint</li>
 *   <li>{@code GRAFANA_INSTANCE_ID}      — Prometheus username (numeric ID)</li>
 *   <li>{@code GRAFANA_API_KEY}          — MetricsPublisher API token</li>
 * </ul>
 */
@Configuration
@EnableScheduling
public class MetricsConfig {

    private static final Logger log = LoggerFactory.getLogger(MetricsConfig.class);

    @Value("${grafana.remote-write.url:}")
    private String remoteWriteUrl;

    @Value("${grafana.remote-write.username:}")
    private String username;

    @Value("${grafana.remote-write.password:}")
    private String password;

    @Value("${grafana.remote-write.enabled:false}")
    private boolean remoteWriteEnabled;

    @Value("${spring.application.name:github-repository-analyzer}")
    private String appName;

    @Value("${SPRING_PROFILES_ACTIVE:local}")
    private String activeProfile;

    private final MeterRegistry meterRegistry;

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ── Common Tags ──────────────────────────────────────────────────────────

    /**
     * Adds {@code application} and {@code environment} tags to ALL metrics.
     * This allows Grafana dashboards to filter by service and environment.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(
                        "application", appName,
                        "environment", activeProfile
                );
    }

    // ── Grafana Cloud Remote Write (Push) ────────────────────────────────────

    /**
     * Pushes current Prometheus metrics to Grafana Cloud every 60 seconds.
     *
     * <p>Uses a plain HTTP POST with Basic Auth (Grafana instance ID + API key).
     * This is the Prometheus remote_write compatible endpoint exposed by Grafana
     * Cloud Mimir.
     *
     * <p>Only runs when {@code grafana.remote-write.enabled=true} AND
     * all required credentials are present.
     */
    @Scheduled(fixedDelayString = "${grafana.remote-write.step-seconds:60}000",
               initialDelayString = "${grafana.remote-write.step-seconds:60}000")
    public void pushMetricsToGrafanaCloud() {
        if (!remoteWriteEnabled) {
            return;
        }

        if (!StringUtils.hasText(remoteWriteUrl)
                || !StringUtils.hasText(username)
                || !StringUtils.hasText(password)) {
            log.warn("[Observability] Grafana remote-write is enabled but credentials are missing. "
                    + "Set GRAFANA_REMOTE_WRITE_URL, GRAFANA_INSTANCE_ID, GRAFANA_API_KEY env vars.");
            return;
        }

        try {
            // Collect metrics in Prometheus text format from MeterRegistry
            String metricsPayload = collectPrometheusMetrics();

            if (!StringUtils.hasText(metricsPayload)) {
                log.debug("[Observability] No metrics to push.");
                return;
            }

            String credentials = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(remoteWriteUrl))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                    .header("X-Scope-OrgID", username)
                    .POST(HttpRequest.BodyPublishers.ofString(metricsPayload))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("[Observability] Metrics pushed to Grafana Cloud successfully. status={}",
                        response.statusCode());
            } else {
                log.warn("[Observability] Grafana Cloud push returned non-2xx status={}, body={}",
                        response.statusCode(), response.body());
            }

        } catch (Exception e) {
            // Non-fatal: metrics push failure must NEVER affect application behaviour
            log.error("[Observability] Failed to push metrics to Grafana Cloud: {}", e.getMessage());
        }
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Scrapes the local /actuator/prometheus endpoint to get
     * Prometheus text-format metrics from the running application.
     *
     * <p>This is simpler and more reliable than iterating MeterRegistry
     * directly — it uses the same serialisation that Prometheus itself would use.
     */
    private String collectPrometheusMetrics() {
        try {
            RestTemplate restTemplate = new RestTemplateBuilder()
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .setReadTimeout(Duration.ofSeconds(10))
                    .build();

            // Scrape our own actuator endpoint on localhost
            return restTemplate.getForObject(
                    "http://localhost:8080/actuator/prometheus",
                    String.class);
        } catch (Exception e) {
            log.warn("[Observability] Could not scrape local prometheus endpoint: {}", e.getMessage());
            return null;
        }
    }
}
