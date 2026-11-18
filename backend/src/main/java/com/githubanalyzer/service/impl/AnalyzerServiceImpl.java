package com.githubanalyzer.service.impl;

import com.githubanalyzer.domain.*;
import com.githubanalyzer.domain.enums.DataSource;
import com.githubanalyzer.dto.github.GitHubContributorDTO;
import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.exception.GitHubApiException;
import com.githubanalyzer.exception.RateLimitExceededException;
import com.githubanalyzer.exception.RepoNotFoundException;
import com.githubanalyzer.config.AppMetrics;
import com.githubanalyzer.service.AnalyzerService;
import com.githubanalyzer.service.TrendsService;
import com.githubanalyzer.service.alert.AlertEngine;
import com.githubanalyzer.service.cache.CacheService;
import com.githubanalyzer.service.enrichment.HateoasLinkEnricher;
import com.githubanalyzer.service.enrichment.LanguageDistributionEnricher;
import com.githubanalyzer.service.github.impl.SpringWebClientGitHubApiClient;
import com.githubanalyzer.service.mock.MockDataProvider;
import com.githubanalyzer.service.scoring.HealthScoreCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.Timer;

// SOLID - Single Responsibility: orchestrates the full analysis pipeline
// SOLID - Open/Closed: new enrichers or score components can be added without touching this class
@Service
public class AnalyzerServiceImpl implements AnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerServiceImpl.class);
    private static final int TOP_CONTRIBUTORS_LIMIT = 5;

    // SOLID - Dependency Inversion: all collaborators are injected via their interfaces
    // This class doesn't know (or care) how caching, scoring, or alerting work internally
    private final SpringWebClientGitHubApiClient githubClient;
    private final CacheService cacheService;
    private final HealthScoreCalculator scoreCalculator;  // Strategy pattern - scoring algorithm is swappable
    private final AlertEngine alertEngine;                // Strategy pattern - alert rules are pluggable
    private final MockDataProvider mockProvider;
    private final LanguageDistributionEnricher languageEnricher;
    private final HateoasLinkEnricher hateoasEnricher;   // HATEOAS - builds _links for self-discovery
    private final TrendsService trendsService;
    private final AppMetrics appMetrics;

    // Constructor injection (preferred over @Autowired) - makes dependencies explicit and testable
    public AnalyzerServiceImpl(
            SpringWebClientGitHubApiClient githubClient,
            CacheService cacheService,
            HealthScoreCalculator scoreCalculator,
            AlertEngine alertEngine,
            MockDataProvider mockProvider,
            LanguageDistributionEnricher languageEnricher,
            HateoasLinkEnricher hateoasEnricher,
            TrendsService trendsService,
            AppMetrics appMetrics) {
        this.githubClient = githubClient;
        this.cacheService = cacheService;
        this.scoreCalculator = scoreCalculator;
        this.alertEngine = alertEngine;
        this.mockProvider = mockProvider;
        this.languageEnricher = languageEnricher;
        this.hateoasEnricher = hateoasEnricher;
        this.trendsService = trendsService;
        this.appMetrics = appMetrics;
    }

    @Override
    public AnalysisResponse analyze(AnalysisRequest request) {
        String owner = request.getOwner();
        String repo = request.getRepo();
        String cacheKey = request.getCacheKey();

        log.info("[ANALYZE] Started for {}/{} | offline={}", owner, repo, request.isOfflineMode());

        // Offline mode: never call the network — serve cache, stale, or mock only
        // This implements Option A (Resilience): explicit degradation strategy
        if (request.isOfflineMode()) {
            Optional<AnalysisResponse> cached = cacheService.get(cacheKey);
            if (cached.isPresent()) {
                log.info("[ANALYZE] Offline cache hit for {}/{} | source=CACHE", owner, repo);
                return withCacheMetadata(cached.get(), DataSource.CACHE, cached.get().getCachedAt());
            }
            log.info("[ANALYZE] Offline no cache for {}/{} | falling back to stale/mock", owner, repo);
            return getOfflineFallback(owner, repo, cacheKey);
        }

        // Online: check cache first to avoid unnecessary GitHub API calls
        // 12-Factor IV (Backing services): cache is treated as an attached resource
        Optional<AnalysisResponse> cached = cacheService.get(cacheKey);
        if (cached.isPresent()) {
            log.info("[ANALYZE] Cache hit for {}/{} | source=CACHE — skipping GitHub API call", owner, repo);
            appMetrics.recordCacheAnalysis();
            return withCacheMetadata(cached.get(), DataSource.CACHE, cached.get().getCachedAt());
        }

        log.info("[ANALYZE] Cache miss for {}/{} | calling GitHub API", owner, repo);

        // Cache miss - call GitHub API, wrapped in a timer for latency metrics
        try {
            AnalysisResponse response = appMetrics.getGitHubApiLatencyTimer()
                    .recordCallable(() -> fetchAnalyzeAndCache(owner, repo, cacheKey));
            appMetrics.recordGitHubApiSuccess();
            appMetrics.recordLiveAnalysis();
            recordAlertMetrics(response);

            log.info("[ANALYZE] Completed for {}/{} | source=LIVE score={} grade={} alerts={}",
                    owner, repo,
                    response.getHealthScore().getScore(),
                    response.getHealthScore().getGrade(),
                    response.getAlerts().size());
            return response;

        } catch (RepoNotFoundException e) {
            // 404 from GitHub - no fallback, surface it to the user
            log.warn("[ANALYZE] Repository not found: {}/{}", owner, repo);
            appMetrics.recordGitHubApiFailure();
            throw e;
        } catch (RateLimitExceededException e) {
            // 429 from GitHub - serve stale data if available
            log.warn("[ANALYZE] Rate limit exceeded for {}/{} | falling back to stale cache", owner, repo);
            appMetrics.recordGitHubRateLimit();
            return getStaleFallback(owner, repo, cacheKey);
        } catch (GitHubApiException e) {
            // Any other GitHub API failure - try stale cache before throwing
            log.warn("[ANALYZE] GitHub API failure for {}/{}: {} | falling back to stale cache", owner, repo, e.getMessage());
            appMetrics.recordGitHubApiFailure();
            return getStaleFallback(owner, repo, cacheKey);
        } catch (Exception e) {
            log.error("[ANALYZE] Unexpected error for {}/{}: {}", owner, repo, e.getMessage(), e);
            appMetrics.recordGitHubApiFailure();
            return getStaleFallback(owner, repo, cacheKey);
        }
    }

    @Override
    public ComparisonResponse compare(AnalysisRequest requestA, AnalysisRequest requestB) {
        log.info("[COMPARE] Started | repoA={} repoB={}", requestA.getFullName(), requestB.getFullName());
        appMetrics.recordComparison();

        // Each analysis goes through its own full degradation chain independently
        AnalysisResponse responseA = analyze(requestA);
        log.info("[COMPARE] Repo A done | {} score={}", requestA.getFullName(), responseA.getHealthScore().getScore());

        AnalysisResponse responseB = analyze(requestB);
        log.info("[COMPARE] Repo B done | {} score={}", requestB.getFullName(), responseB.getHealthScore().getScore());

        ComparisonResponse result = new ComparisonResponse(responseA, responseB);
        log.info("[COMPARE] Completed | winner={}", result.getWinner());
        return result;
    }

    private AnalysisResponse fetchAnalyzeAndCache(String owner, String repo, String cacheKey) {
        log.info("[GITHUB] Fetching repo details for {}/{}", owner, repo);

        // Step 1: repo details must succeed - throws RepoNotFoundException on 404
        var repoDetails = githubClient.fetchRepoDetails(owner, repo);
        log.info("[GITHUB] Repo details received for {}/{} | stars={} issues={}",
                owner, repo, repoDetails.getStargazersCount(), repoDetails.getOpenIssuesCount());

        // Step 2: other 4 GitHub endpoints run in parallel using Mono.zip (reactor)
        // This cuts response time from ~15s (serial) to ~4s (parallel)
        // Failures on any individual call return empty collections - non-fatal
        log.info("[GITHUB] Starting parallel fetch for {}/{} | endpoints: contributors, languages, commits, contents", owner, repo);
        long parallelStart = System.currentTimeMillis();

        var supporting = githubClient.fetchSupportingDataParallel(owner, repo);

        long parallelElapsed = System.currentTimeMillis() - parallelStart;
        log.info("[GITHUB] Parallel fetch complete for {}/{} | elapsed={}ms | contributors={} languages={} commits={} contents={}",
                owner, repo, parallelElapsed,
                supporting.contributors().size(),
                supporting.languages().size(),
                supporting.recentCommits().size(),
                supporting.rootContents().size());

        log.info("[ENRICH] Building enriched response for {}/{}", owner, repo);

        RepoData repoData = RepoData.builder()
                .owner(owner).repo(repo)
                .repoDetails(repoDetails)
                .contributors(supporting.contributors())
                .languages(supporting.languages())
                .recentCommits(supporting.recentCommits())
                .rootContents(supporting.rootContents())
                .build();

        AnalysisResponse response = buildEnrichedResponse(repoData, DataSource.LIVE, null);

        log.info("[ENRICH] Response built for {}/{} | score={} grade={} alerts={} source=LIVE",
                owner, repo,
                response.getHealthScore().getScore(),
                response.getHealthScore().getGrade(),
                response.getAlerts().size());

        // 12-Factor IV: cache is a backing service - store result for subsequent requests
        cacheService.put(cacheKey, response);
        log.info("[CACHE] Stored result for {}/{}", owner, repo);

        // Record trend snapshot - non-fatal if it fails
        try {
            trendsService.recordSnapshot(owner, repo, response);
            log.info("[TRENDS] Snapshot recorded for {}/{}", owner, repo);
        } catch (Exception e) {
            log.warn("[TRENDS] Failed to record snapshot for {}/{}: {}", owner, repo, e.getMessage());
        }

        return response;
    }

    private AnalysisResponse buildEnrichedResponse(RepoData repoData, DataSource source, Instant cachedAt) {
        var repoDetails = repoData.getRepoDetails();

        RepoStats stats = RepoStats.builder()
                .stars(repoDetails.getStargazersCount())
                .forks(repoDetails.getForksCount())
                .watchers(repoDetails.getWatchersCount())
                .openIssues(repoDetails.getOpenIssuesCount())
                .defaultBranch(repoDetails.getDefaultBranch())
                .description(repoDetails.getDescription())
                .license(repoDetails.getLicenseName())
                .hasWiki(repoDetails.isHasWiki())
                .hasTopics(repoDetails.isHasTopics())
                .homepage(repoDetails.getHomepage())
                .createdAt(repoDetails.getCreatedAt())
                .updatedAt(repoDetails.getUpdatedAt())
                .pushedAt(repoDetails.getPushedAt())
                .build();

        return AnalysisResponse.builder()
                .owner(repoData.getOwner())
                .repo(repoData.getRepo())
                .stats(stats)
                .healthScore(scoreCalculator.calculate(repoData))   // Strategy pattern
                .alerts(alertEngine.evaluate(repoData))             // Chain of Responsibility pattern
                .languages(languageEnricher.enrich(repoData.getLanguages()))
                .contributorActivity(buildContributorActivity(repoData))
                .source(source)
                .cachedAt(cachedAt)
                .links(hateoasEnricher.buildLinks(repoData.getOwner(), repoData.getRepo())) // HATEOAS _links
                .build();
    }

    private ContributorActivity buildContributorActivity(RepoData repoData) {
        // Filter out contributors where GitHub returned null author info
        List<GitHubContributorDTO> validContributors = repoData.getContributors().stream()
                .filter(c -> c.getAuthor() != null)
                .collect(Collectors.toList());

        List<ContributorSummary> topContributors = validContributors.stream()
                .sorted((a, b) -> Integer.compare(b.getTotal(), a.getTotal()))
                .limit(TOP_CONTRIBUTORS_LIMIT)
                .map(c -> new ContributorSummary(c.getAuthor().getLogin(), c.getTotal(), c.getActiveWeeks()))
                .collect(Collectors.toList());

        int totalCommits = validContributors.stream()
                .mapToInt(c -> c.getWeeks() != null
                        ? c.getWeeks().stream().mapToInt(w -> w.getCommits()).sum()
                        : 0)
                .sum();

        return ContributorActivity.builder()
                .totalContributors(validContributors.size())
                .topContributors(topContributors)
                .totalCommitsLast52Weeks(totalCommits)
                .lastCommitDate(repoData.getLastCommitDate())
                .build();
    }

    // Resilience (Option A) - degradation chain for offline mode:
    // fresh cache (CACHE) -> expired cache (STALE) -> demo data (MOCK)
    private AnalysisResponse getOfflineFallback(String owner, String repo, String cacheKey) {
        Optional<AnalysisResponse> stale = cacheService.getStale(cacheKey);
        if (stale.isPresent()) {
            log.info("[FALLBACK] Offline stale cache served for {}/{} | source=STALE", owner, repo);
            appMetrics.recordStaleAnalysis();
            return withNotice(
                    withCacheMetadata(stale.get(), DataSource.STALE, stale.get().getCachedAt()),
                    "You are in offline mode. Showing previously cached data for this repository.");
        }
        // Null Object pattern: mockProvider always returns a valid response, never throws
        log.info("[FALLBACK] Offline no cache for {}/{} | source=MOCK", owner, repo);
        appMetrics.recordMockAnalysis();
        return withNotice(
                mockProvider.getMockData(owner, repo),
                "No saved data for " + owner + "/" + repo + " in offline mode. Showing demo data.");
    }

    // Resilience (Option A) - online failure degradation:
    // stale cache only (no mock data online - fabricated numbers must not appear in live mode)
    private AnalysisResponse getStaleFallback(String owner, String repo, String cacheKey) {
        Optional<AnalysisResponse> stale = cacheService.getStale(cacheKey);
        if (stale.isPresent()) {
            log.info("[FALLBACK] API failure — serving stale cache for {}/{} | source=STALE", owner, repo);
            appMetrics.recordStaleAnalysis();
            return withNotice(
                    withCacheMetadata(stale.get(), DataSource.STALE, stale.get().getCachedAt()),
                    "GitHub is unreachable right now. Showing the last saved data (may be outdated).");
        }
        log.error("[FALLBACK] API failure + no cache for {}/{} | throwing 503", owner, repo);
        throw new GitHubApiException(
                "GitHub is currently unreachable and no cached data exists for this repository.", 503, null);
    }

    private void recordAlertMetrics(AnalysisResponse response) {
        if (response.getAlerts() == null) return;
        response.getAlerts().forEach(alert -> {
            if (alert.getType() == null) return;
            switch (alert.getType()) {
                case INACTIVE_REPO             -> appMetrics.recordInactiveRepoAlert();
                case HIGH_ISSUE_BACKLOG        -> appMetrics.recordHighIssueBacklogAlert();
                case NO_TESTS                  -> appMetrics.recordNoTestInfraAlert();
                case SECURITY_VULNERABILITIES  -> appMetrics.recordSecurityVulnAlert();
                default -> log.debug("Untracked alert type: {}", alert.getType());
            }
        });
    }

    // Returns a copy of the response with source/cachedAt overridden
    // Avoids mutating shared state - immutability via builder
    private AnalysisResponse withCacheMetadata(AnalysisResponse original, DataSource source, Instant cachedAt) {
        return AnalysisResponse.builder()
                .owner(original.getOwner()).repo(original.getRepo())
                .stats(original.getStats()).healthScore(original.getHealthScore())
                .alerts(original.getAlerts()).languages(original.getLanguages())
                .contributorActivity(original.getContributorActivity())
                .source(source)
                .cachedAt(cachedAt != null ? cachedAt : Instant.now())
                .notice(original.getNotice()).links(original.getLinks())
                .build();
    }

    // Returns a copy of the response with a user-facing notice attached
    private AnalysisResponse withNotice(AnalysisResponse original, String notice) {
        return AnalysisResponse.builder()
                .owner(original.getOwner()).repo(original.getRepo())
                .stats(original.getStats()).healthScore(original.getHealthScore())
                .alerts(original.getAlerts()).languages(original.getLanguages())
                .contributorActivity(original.getContributorActivity())
                .source(original.getSource()).cachedAt(original.getCachedAt())
                .notice(notice).links(original.getLinks())
                .build();
    }
}
