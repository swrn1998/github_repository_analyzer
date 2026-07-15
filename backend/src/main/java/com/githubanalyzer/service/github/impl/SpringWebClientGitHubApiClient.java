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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Spring WebClient-based implementation of {@link GitHubApiClient}.
 *
 * <p>Uses Spring's non-blocking {@code WebClient} with {@code .block()} calls
 * to keep the service layer synchronous while benefiting from WebClient's
 * richer error handling, retry, and filter capabilities.
 *
 * <p>All requests include the Authorization header when a token is configured.
 * Without a token: 60 req/hr. With token: 5000 req/hr.
 */
@Component
public class SpringWebClientGitHubApiClient implements GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(SpringWebClientGitHubApiClient.class);

    private static final int STATS_MAX_RETRIES = 3;
    private static final Duration STATS_RETRY_DELAY = Duration.ofSeconds(2);

    private final WebClient webClient;
    private final String githubToken;

    public SpringWebClientGitHubApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${github.api.base-url}") String baseUrl,
            @Value("${github.api.token:}") String githubToken) {
        this.githubToken = githubToken;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
        
        if (StringUtils.hasText(githubToken)) {
            log.info("GitHub API client initialized with authentication token (rate limit: 5000 req/hr)");
        } else {
            log.warn("GitHub API client initialized WITHOUT token - rate limit: 60 req/hr per IP");
        }
    }

    // ── Public API ──────────────────────────────────────────────────────────

    @Override
    public GitHubRepoDTO fetchRepoDetails(String owner, String repo) {
        String path = "/repos/{owner}/{repo}";
        log.debug("Fetching repo details for {}/{}", owner, repo);

        try {
            return buildRequest()
                    .uri(path, owner, repo)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            log.error("Repository not found: {}/{}", owner, repo);
                            return response.createException()
                                    .map(e -> new RepoNotFoundException(owner, repo));
                        }
                        if (response.statusCode().value() == 429) {
                            log.error("Rate limit exceeded for {}/{}", owner, repo);
                            return response.createException()
                                    .map(e -> new RateLimitExceededException(
                                            extractRateLimitReset(e)));
                        }
                        log.error("GitHub API 4xx error for {}/{}: status={}", owner, repo, response.statusCode().value());
                        return response.createException()
                                .map(e -> {
                                    log.error("Error body: {}", e.getResponseBodyAsString());
                                    return new GitHubApiException(
                                            "GitHub API client error", e.getStatusCode().value(), e.getResponseBodyAsString());
                                });
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("GitHub API 5xx error for {}/{}: status={}", owner, repo, response.statusCode().value());
                        return response.createException()
                                .map(e -> {
                                    log.error("Error body: {}", e.getResponseBodyAsString());
                                    return new GitHubApiException(
                                            "GitHub API server error", e.getStatusCode().value(), e.getResponseBodyAsString());
                                });
                    })
                    .bodyToMono(GitHubRepoDTO.class)
                    .block();
        } catch (RepoNotFoundException | RateLimitExceededException | GitHubApiException e) {
            log.error("Domain exception for {}/{}: {}", owner, repo, e.getMessage());
            throw e;   // Already a domain exception — don't re-wrap (preserves status code)
        } catch (WebClientResponseException e) {
            log.error("WebClient exception for {}/{}: status={}, message={}", 
                    owner, repo, e.getStatusCode().value(), e.getMessage());
            throw new GitHubApiException("GitHub API request failed: " + e.getMessage(),
                    e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected exception for {}/{}: {}", owner, repo, e.getMessage(), e);
            throw new GitHubApiException("GitHub API connection failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GitHubContributorDTO> fetchContributors(String owner, String repo) {
        log.debug("Fetching contributors for {}/{}", owner, repo);

        try {
            // GitHub returns 202 while computing stats — retry with backoff
            List<GitHubContributorDTO> contributors = buildRequest()
                    .uri("/repos/{owner}/{repo}/stats/contributors", owner, repo)
                    .retrieve()
                    .onStatus(status -> status.value() == 202,
                            response -> {
                                log.debug("GitHub stats computing (202), will retry...");
                                return response.createException();
                            })
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubContributorDTO>>() {})
                    .retryWhen(Retry.fixedDelay(STATS_MAX_RETRIES, STATS_RETRY_DELAY)
                            .filter(e -> e.getMessage() != null && e.getMessage().contains("202")))
                    .doOnSuccess(result -> {
                        if (result != null && !result.isEmpty()) {
                            log.info("Received {} contributors for {}/{}", result.size(), owner, repo);
                            long withAuthor = result.stream().filter(c -> c.getAuthor() != null).count();
                            long withoutAuthor = result.size() - withAuthor;
                            log.info("Contributors with author info: {}, without: {}", withAuthor, withoutAuthor);
                            
                            // Log a sample contributor
                            if (!result.isEmpty()) {
                                GitHubContributorDTO sample = result.get(0);
                                log.debug("Sample contributor - total: {}, hasAuthor: {}, weeksCount: {}", 
                                        sample.getTotal(), 
                                        sample.getAuthor() != null,
                                        sample.getWeeks() != null ? sample.getWeeks().size() : 0);
                            }
                        } else {
                            log.warn("Received empty or null contributor list for {}/{}", owner, repo);
                        }
                    })
                    .onErrorReturn(Collections.emptyList())
                    .block();
            
            return contributors != null ? contributors : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch contributors for {}/{}: {}", owner, repo, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Long> fetchLanguages(String owner, String repo) {
        log.debug("Fetching languages for {}/{}", owner, repo);

        try {
            Map<String, Long> result = buildRequest()
                    .uri("/repos/{owner}/{repo}/languages", owner, repo)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {})
                    .block();
            return result != null ? result : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to fetch languages for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public List<GitHubCommitDTO> fetchRecentCommits(String owner, String repo, int perPage) {
        log.debug("Fetching recent commits for {}/{} (perPage={})", owner, repo, perPage);

        try {
            List<GitHubCommitDTO> result = buildRequest()
                    .uri("/repos/{owner}/{repo}/commits?per_page={perPage}", owner, repo, perPage)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubCommitDTO>>() {})
                    .onErrorResume(e -> {
                        // GitHub API may return an object instead of array (error response or single commit)
                        log.debug("Array deserialization failed for commits, attempting single object for {}/{}: {}", 
                                owner, repo, e.getMessage());
                        return buildRequest()
                                .uri("/repos/{owner}/{repo}/commits?per_page={perPage}", owner, repo, perPage)
                                .retrieve()
                                .bodyToMono(GitHubCommitDTO.class)
                                .map(Collections::singletonList)
                                .onErrorReturn(Collections.emptyList());
                    })
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch commits for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<GitHubContentDTO> fetchRootContents(String owner, String repo) {
        log.debug("Fetching root contents for {}/{}", owner, repo);

        try {
            List<GitHubContentDTO> result = buildRequest()
                    .uri("/repos/{owner}/{repo}/contents", owner, repo)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubContentDTO>>() {})
                    .onErrorResume(e -> {
                        // GitHub API may return an object instead of array for single files or errors
                        log.debug("Array deserialization failed, attempting single object for {}/{}: {}", 
                                owner, repo, e.getMessage());
                        return buildRequest()
                                .uri("/repos/{owner}/{repo}/contents", owner, repo)
                                .retrieve()
                                .bodyToMono(GitHubContentDTO.class)
                                .map(Collections::singletonList)
                                .onErrorReturn(Collections.emptyList());
                    })
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch root contents for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    /**
     * Builds a WebClient request spec with the Authorization header
     * injected only when a token is configured.
     */
    private WebClient.RequestHeadersUriSpec<?> buildRequest() {
        return authenticatedClient().get();
    }

    /**
     * Creates a mutated WebClient that includes the Bearer token header.
     * Called per-request so the token is applied to every call.
     */
    private WebClient authenticatedClient() {
        if (StringUtils.hasText(githubToken)) {
            return webClient.mutate()
                    .defaultHeader("Authorization", "Bearer " + githubToken)
                    .build();
        }
        return webClient;
    }

    private long extractRateLimitReset(WebClientResponseException e) {
        try {
            String resetHeader = e.getHeaders().getFirst("X-RateLimit-Reset");
            return resetHeader != null ? Long.parseLong(resetHeader) : 0L;
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
