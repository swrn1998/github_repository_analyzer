package com.githubanalyzer.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Spring WebClient configuration for GitHub API calls.
 *
 * <p>Configures connection and read timeouts via Netty's HTTP client.
 * These are critical for the resilience strategy — without timeouts,
 * a slow GitHub API response would block the service indefinitely.
 */
@Configuration
public class WebClientConfig {

    @Value("${github.api.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${github.api.read-timeout-ms:10000}")
    private int readTimeoutMs;

    /**
     * Creates a WebClient.Builder bean with configured timeouts.
     * Downstream components (e.g., SpringWebClientGitHubApiClient) use this
     * builder to create their own WebClient instances with a base URL.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs))
                .followRedirect(true)  // Enable automatic redirect following
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                );

        // Configure buffer size to handle large responses (64MB for large repos like facebook/react)
        // The contributor stats endpoint can return very large responses for popular repos
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(64 * 1024 * 1024)) // 64MB buffer size
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies);
    }
}
