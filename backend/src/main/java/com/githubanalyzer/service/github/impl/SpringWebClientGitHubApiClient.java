package com.githubanalyzer.service.github.impl;

import com.githubanalyzer.dto.github.*;
import com.githubanalyzer.exception.GitHubApiException;
import com.githubanalyzer.exception.RateLimitExceededException;
import com.githubanalyzer.exception.RepoNotFoundException;
import com.githubanalyzer.service.github.GitHubApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// SOLID - Single Responsibility: only handles GitHub REST API communication
// SOLID - Dependency Inversion: implements GitHubApiClient interface so callers don't depend on this class directly
// 12-Factor IV (Backing services): GitHub API is treated as an attached resource accessed over the network
@Component
public class SpringWebClientGitHubApiClient implements GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(SpringWebClientGitHubApiClient.class);

    // GitHub's /stats/contributors returns 202 while computing - retry up to 3 times with 2s delay
    private static final int STATS_MAX_RETRIES = 3;
    private static final Duration STATS_RETRY_DELAY = Duration.ofSeconds(2);

    private final WebClient webClient;

    // 12-Factor III (Config): token read from environment variable, never hardcoded
    // Without a token: 60 req/hr. With token: 5000 req/hr
    private final String githubToken;

    public SpringWebClientGitHubApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${github.api.base-url}") String baseUrl,     // 12-Factor III: base URL from config
            @Value("${github.api.token:}") String githubToken) { // empty default = unauthenticated mode
        this.githubToken = githubToken;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
        if (StringUtils.hasText(githubToken)) {
            log.info("GitHub API client: authenticated (5000 req/hr)");
        } else {
            log.warn("GitHub API client: no token set — limited to 60 req/hr");
        }
    }

    // This is the only call that is FATAL on failure (throws instead of returning empty)
    // 404 = repo doesn't exist, should not be swallowed
    @Override
    public GitHubRepoDTO fetchRepoDetails(String owner, String repo) {
        log.info("[API CALL] --> GET /repos/{}/{}", owner, repo);
        long start = System.currentTimeMillis();
        try {
            GitHubRepoDTO result = buildRequest()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            return response.createException().map(e -> new RepoNotFoundException(owner, repo));
                        }
                        if (response.statusCode().value() == 429) {
                            // Rate limit hit - extract reset time from header for the caller
                            return response.createException()
                                    .map(e -> new RateLimitExceededException(extractRateLimitReset(e)));
                        }
                        return response.createException()
                                .map(e -> new GitHubApiException("GitHub API client error",
                                        e.getStatusCode().value(), e.getResponseBodyAsString()));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            response.createException()
                                    .map(e -> new GitHubApiException("GitHub API server error",
                                            e.getStatusCode().value(), e.getResponseBodyAsString())))
                    .bodyToMono(GitHubRepoDTO.class)
                    .block(); // block() converts reactive Mono to synchronous result

            log.info("[API CALL] <-- GET /repos/{}/{} | elapsed={}ms", owner, repo, System.currentTimeMillis() - start);
            return result;

        } catch (RepoNotFoundException | RateLimitExceededException | GitHubApiException e) {
            log.warn("[API CALL] <-- GET /repos/{}/{} | FAILED elapsed={}ms reason={}",
                    owner, repo, System.currentTimeMillis() - start, e.getMessage());
            throw e; // re-throw domain exceptions as-is, don't wrap them
        } catch (WebClientResponseException e) {
            log.error("[API CALL] <-- GET /repos/{}/{} | HTTP {} elapsed={}ms",
                    owner, repo, e.getStatusCode().value(), System.currentTimeMillis() - start);
            throw new GitHubApiException("GitHub API request failed: " + e.getMessage(),
                    e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[API CALL] <-- GET /repos/{}/{} | ERROR elapsed={}ms msg={}",
                    owner, repo, System.currentTimeMillis() - start, e.getMessage());
            throw new GitHubApiException("GitHub API connection failed: " + e.getMessage(), e);
        }
    }

    // Non-fatal: returns empty list on any failure so the analysis pipeline can continue
    @Override
    public List<GitHubContributorDTO> fetchContributors(String owner, String repo) {
        try {
            List<GitHubContributorDTO> result = buildRequest()
                    .uri("/repos/{owner}/{repo}/stats/contributors", owner, repo)
                    .retrieve()
                    // GitHub returns 202 while stats are still being computed server-side
                    .onStatus(status -> status.value() == 202, response -> response.createException())
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubContributorDTO>>() {})
                    // Retry on 202 - filter ensures we only retry for "computing" errors, not real failures
                    .retryWhen(Retry.fixedDelay(STATS_MAX_RETRIES, STATS_RETRY_DELAY)
                            .filter(e -> e.getMessage() != null && e.getMessage().contains("202")))
                    .onErrorReturn(Collections.emptyList())
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("[API CALL] Contributors fetch failed for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyList();
        }
    }

    // Non-fatal: empty map means language distribution won't be shown in the UI
    @Override
    public Map<String, Long> fetchLanguages(String owner, String repo) {
        try {
            Map<String, Long> result = buildRequest()
                    .uri("/repos/{owner}/{repo}/languages", owner, repo)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {})
                    .block();
            return result != null ? result : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("[API CALL] Languages fetch failed for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public List<GitHubCommitDTO> fetchRecentCommits(String owner, String repo, int perPage) {
        try {
            List<GitHubCommitDTO> result = buildRequest()
                    .uri("/repos/{owner}/{repo}/commits?per_page={perPage}", owner, repo, perPage)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubCommitDTO>>() {})
                    // Some repos return a single object instead of array - try single-item fallback
                    .onErrorResume(e -> buildRequest()
                            .uri("/repos/{owner}/{repo}/commits?per_page={perPage}", owner, repo, perPage)
                            .retrieve()
                            .bodyToMono(GitHubCommitDTO.class)
                            .map(Collections::singletonList)
                            .onErrorReturn(Collections.emptyList()))
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("[API CALL] Commits fetch failed for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<GitHubContentDTO> fetchRootContents(String owner, String repo) {
        try {
            List<GitHubContentDTO> result = buildRequest()
                    .uri("/repos/{owner}/{repo}/contents", owner, repo)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubContentDTO>>() {})
                    // Single-file repos return an object instead of array - handle gracefully
                    .onErrorResume(e -> buildRequest()
                            .uri("/repos/{owner}/{repo}/contents", owner, repo)
                            .retrieve()
                            .bodyToMono(GitHubContentDTO.class)
                            .map(Collections::singletonList)
                            .onErrorReturn(Collections.emptyList()))
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("[API CALL] Root contents fetch failed for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyList();
        }
    }

    // Mono.zip fires all 4 requests simultaneously and waits for all to complete
    // This cuts response time from ~15s (serial) to ~4s (parallel)
    // Each Mono has its own error handler so one failure doesn't cancel the others
    public SupportingData fetchSupportingDataParallel(String owner, String repo) {
        log.info("[PARALLEL] --> Starting 4 concurrent GitHub API calls for {}/{}", owner, repo);
        log.info("[PARALLEL]     [1] /stats/contributors");
        log.info("[PARALLEL]     [2] /languages");
        log.info("[PARALLEL]     [3] /commits?per_page=5");
        log.info("[PARALLEL]     [4] /contents");

        Mono<List<GitHubContributorDTO>> contributorsMono = buildRequest()
                .uri("/repos/{owner}/{repo}/stats/contributors", owner, repo)
                .retrieve()
                .onStatus(status -> status.value() == 202, response -> response.createException())
                .bodyToMono(new ParameterizedTypeReference<List<GitHubContributorDTO>>() {})
                .retryWhen(Retry.fixedDelay(STATS_MAX_RETRIES, STATS_RETRY_DELAY)
                        .filter(e -> e.getMessage() != null && e.getMessage().contains("202")))
                .onErrorReturn(Collections.emptyList())
                .doOnSuccess(r -> log.info("[PARALLEL]     [1] /stats/contributors done | count={}", r != null ? r.size() : 0))
                .doOnError(e -> log.warn("[PARALLEL]     [1] /stats/contributors FAILED: {}", e.getMessage()));

        Mono<Map<String, Long>> languagesMono = buildRequest()
                .uri("/repos/{owner}/{repo}/languages", owner, repo)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {})
                .onErrorReturn(Collections.emptyMap())
                .doOnSuccess(r -> log.info("[PARALLEL]     [2] /languages done | count={}", r != null ? r.size() : 0))
                .doOnError(e -> log.warn("[PARALLEL]     [2] /languages FAILED: {}", e.getMessage()));

        Mono<List<GitHubCommitDTO>> commitsMono = buildRequest()
                .uri("/repos/{owner}/{repo}/commits?per_page=5", owner, repo)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<GitHubCommitDTO>>() {})
                .onErrorReturn(Collections.emptyList())
                .doOnSuccess(r -> log.info("[PARALLEL]     [3] /commits done | count={}", r != null ? r.size() : 0))
                .doOnError(e -> log.warn("[PARALLEL]     [3] /commits FAILED: {}", e.getMessage()));

        Mono<List<GitHubContentDTO>> contentsMono = buildRequest()
                .uri("/repos/{owner}/{repo}/contents", owner, repo)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<GitHubContentDTO>>() {})
                .onErrorResume(e -> buildRequest()
                        .uri("/repos/{owner}/{repo}/contents", owner, repo)
                        .retrieve()
                        .bodyToMono(GitHubContentDTO.class)
                        .map(Collections::singletonList)
                        .onErrorReturn(Collections.emptyList()))
                .doOnSuccess(r -> log.info("[PARALLEL]     [4] /contents done | count={}", r != null ? r.size() : 0))
                .doOnError(e -> log.warn("[PARALLEL]     [4] /contents FAILED: {}", e.getMessage()));

        // zip combines all 4 Monos - blocks until all complete or any fails fatally
        SupportingData result = Mono.zip(contributorsMono, languagesMono, commitsMono, contentsMono)
                .map(tuple -> new SupportingData(
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3(),
                        tuple.getT4()))
                .block();

        log.info("[PARALLEL] <-- All 4 calls complete for {}/{}", owner, repo);
        return result;
    }

    // Java 16+ record - immutable container for the 4 parallel fetch results
    public record SupportingData(
            List<GitHubContributorDTO> contributors,
            Map<String, Long> languages,
            List<GitHubCommitDTO> recentCommits,
            List<GitHubContentDTO> rootContents) {}

    // Injects the Bearer token only when configured - supports both authenticated and anonymous modes
    private WebClient.RequestHeadersUriSpec<?> buildRequest() {
        if (StringUtils.hasText(githubToken)) {
            return webClient.mutate()
                    .defaultHeader("Authorization", "Bearer " + githubToken)
                    .build()
                    .get();
        }
        return webClient.get();
    }

    private long extractRateLimitReset(WebClientResponseException e) {
        try {
            String reset = e.getHeaders().getFirst("X-RateLimit-Reset");
            return reset != null ? Long.parseLong(reset) : 0L;
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
