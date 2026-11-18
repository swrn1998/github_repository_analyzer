package com.githubanalyzer.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

// SOLID - Single Responsibility: only configures the WebClient HTTP client
// 12-Factor III (Config): all timeouts come from environment variables with sensible defaults
@Configuration
public class WebClientConfig {

    // 12-Factor III: timeout values can be overridden per environment without rebuilding
    @Value("${github.api.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${github.api.read-timeout-ms:60000}")
    private int readTimeoutMs;

    // @Bean registers this as a singleton in the Spring context
    // SpringWebClientGitHubApiClient receives this builder via constructor injection
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs) // TCP connection timeout
                .responseTimeout(Duration.ofMillis(readTimeoutMs))              // full response timeout
                .followRedirect(true)
                .doOnConnected(conn -> conn
                        // ReadTimeoutHandler fires if no data is received within the window
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                        // WriteTimeoutHandler fires if the request can't be sent within 5s
                        .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                );

        // GitHub contributor stats for large repos (facebook/react) can be several MB
        // Default WebClient buffer is 256KB - increasing to 64MB prevents codec exceptions
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(64 * 1024 * 1024)) // 64MB
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies);
    }
}
