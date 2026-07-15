package com.githubanalyzer.service.impl;

import com.githubanalyzer.domain.*;
import com.githubanalyzer.domain.enums.DataSource;
import com.githubanalyzer.dto.github.GitHubContributorDTO;
import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.exception.GitHubApiException;
import com.githubanalyzer.exception.RateLimitExceededException;
import com.githubanalyzer.exception.RepoNotFoundException;
import com.githubanalyzer.service.AnalyzerService;
import com.githubanalyzer.service.TrendsService;
import com.githubanalyzer.service.alert.AlertEngine;
import com.githubanalyzer.service.cache.CacheService;
import com.githubanalyzer.service.enrichment.HateoasLinkEnricher;
import com.githubanalyzer.service.enrichment.LanguageDistributionEnricher;
import com.githubanalyzer.service.github.GitHubApiClient;
import com.githubanalyzer.service.mock.MockDataProvider;
import com.githubanalyzer.service.scoring.HealthScoreCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Core implementation of {@link AnalyzerService}.
 *
 * <p>Orchestrates the full analysis pipeline using the Facade pattern:
 * <ol>
 *   <li>Cache check</li>
 *   <li>Offline / degradation mode handling</li>
 *   <li>GitHub API calls (parallel where possible)</li>
 *   <li>Data enrichment (scoring, alerts, language distribution)</li>
 *   <li>HATEOAS link building</li>
 *   <li>Cache population</li>
 * </ol>
 *
 * <p>This class has no knowledge of how each collaborator works internally —
 * all dependencies are programming to interfaces (Dependency Inversion Principle).
 */
@Service
public class AnalyzerServiceImpl implements AnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerServiceImpl.class);

    private static final int RECENT_COMMIT_COUNT = 5;
    private static final int TOP_CONTRIBUTORS_LIMIT = 5;

    private final GitHubApiClient githubClient;
    private final CacheService cacheService;
    private final HealthScoreCalculator scoreCalculator;
    private final AlertEngine alertEngine;
    private final MockDataProvider mockProvider;
    private final LanguageDistributionEnricher languageEnricher;
    private final HateoasLinkEnricher hateoasEnricher;
    private final TrendsService trendsService;

    public AnalyzerServiceImpl(
            GitHubApiClient githubClient,
            CacheService cacheService,
            HealthScoreCalculator scoreCalculator,
            AlertEngine alertEngine,
            MockDataProvider mockProvider,
            LanguageDistributionEnricher languageEnricher,
            HateoasLinkEnricher hateoasEnricher,
            TrendsService trendsService) {
        this.githubClient = githubClient;
        this.cacheService = cacheService;
        this.scoreCalculator = scoreCalculator;
        this.alertEngine = alertEngine;
        this.mockProvider = mockProvider;
        this.languageEnricher = languageEnricher;
        this.hateoasEnricher = hateoasEnricher;
        this.trendsService = trendsService;
    }

    // ── Public API ──────────────────────────────────────────────────────────

    @Override
    public AnalysisResponse analyze(AnalysisRequest request) {
        String owner = request.getOwner();
        String repo = request.getRepo();
        String cacheKey = request.getCacheKey();

        log.info("Analyzing repository: {} (offline={})", request.getFullName(), request.isOfflineMode());

        // ── Offline mode: CACHE-FIRST, never call the network ─────────────
        // Serve fresh cache (CACHE) → expired cache (STALE) → demo data (MOCK).
        if (request.isOfflineMode()) {
            Optional<AnalysisResponse> cached = cacheService.get(cacheKey);
            if (cached.isPresent()) {
                log.info("Offline + fresh cache hit for {}", request.getFullName());
                return withCacheMetadata(cached.get(), DataSource.CACHE, cached.get().getCachedAt());
            }
            log.info("Offline + no fresh cache for {} — using stale/mock fallback", request.getFullName());
            return getOfflineFallback(owner, repo, cacheKey);
        }

        // ── Online mode: CACHE-FIRST with refresh on expiry ────────────────
        // Check cache first. If fresh, return immediately.
        // If expired or missing, fetch from GitHub API and cache the result.
        // If API fails, fall back to stale cache or error.
        Optional<AnalysisResponse> cached = cacheService.get(cacheKey);
        if (cached.isPresent()) {
            log.info("Online + fresh cache hit for {} — returning cached data", request.getFullName());
            return withCacheMetadata(cached.get(), DataSource.CACHE, cached.get().getCachedAt());
        }

        // No fresh cache — fetch from GitHub API
        log.debug("Online + cache miss/expired for {} — fetching from GitHub API", request.getFullName());
        try {
            return fetchAnalyzeAndCache(owner, repo, cacheKey);
        } catch (RepoNotFoundException e) {
            // Invalid / non-existent repo while online — surface a clear 404 to the user.
            throw e;
        } catch (RateLimitExceededException e) {
            log.warn("GitHub rate limit exceeded for {} — falling back to stale/mock", request.getFullName());
            return getStaleFallback(owner, repo, cacheKey);
        } catch (GitHubApiException e) {
            log.warn("GitHub API failure for {}: {} — falling back to stale/mock",
                    request.getFullName(), e.getMessage());
            return getStaleFallback(owner, repo, cacheKey);
        } catch (Exception e) {
            log.error("Unexpected error analyzing {}: {}", request.getFullName(), e.getMessage(), e);
            return getStaleFallback(owner, repo, cacheKey);
        }
    }

    @Override
    public ComparisonResponse compare(AnalysisRequest requestA, AnalysisRequest requestB) {
        log.info("Comparing {} vs {}", requestA.getFullName(), requestB.getFullName());

        // Analyze both repos — each goes through the full degradation chain independently
        AnalysisResponse responseA = analyze(requestA);
        AnalysisResponse responseB = analyze(requestB);

        return new ComparisonResponse(responseA, responseB);
    }

    // ── Private pipeline ────────────────────────────────────────────────────

    /**
     * Full live pipeline: fetch all GitHub data → enrich → cache → return.
     */
    private AnalysisResponse fetchAnalyzeAndCache(String owner, String repo, String cacheKey) {
        log.debug("Fetching live data for {}/{}", owner, repo);

        // Fetch repo details (throws RepoNotFoundException if 404)
        var repoDetails = githubClient.fetchRepoDetails(owner, repo);

        // Fetch supporting data — failures here are non-fatal (return empty collections)
        var contributors = githubClient.fetchContributors(owner, repo);
        var languages = githubClient.fetchLanguages(owner, repo);
        var recentCommits = githubClient.fetchRecentCommits(owner, repo, RECENT_COMMIT_COUNT);
        var rootContents = githubClient.fetchRootContents(owner, repo);

        // Aggregate raw data
        RepoData repoData = RepoData.builder()
                .owner(owner).repo(repo)
                .repoDetails(repoDetails)
                .contributors(contributors)
                .languages(languages)
                .recentCommits(recentCommits)
                .rootContents(rootContents)
                .build();

        // Enrich
        AnalysisResponse response = buildEnrichedResponse(repoData, DataSource.LIVE, null);

        // Cache the result
        cacheService.put(cacheKey, response);
        
        // Record trend snapshot for historical tracking
        try {
            trendsService.recordSnapshot(owner, repo, response);
        } catch (Exception e) {
            log.warn("Failed to record trend snapshot for {}/{}: {}", owner, repo, e.getMessage());
            // Non-fatal — continue even if trend recording fails
        }
        
        log.info("Analysis complete for {}/{}: score={}, grade={}, alerts={}",
                owner, repo,
                response.getHealthScore().getScore(),
                response.getHealthScore().getGrade(),
                response.getAlerts().size());

        return response;
    }

    /**
     * Builds the full enriched {@link AnalysisResponse} from raw data.
     */
    private AnalysisResponse buildEnrichedResponse(RepoData repoData,
                                                    DataSource source,
                                                    Instant cachedAt) {
        var repoDetails = repoData.getRepoDetails();

        // 1. Core stats
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

        // 2. Health score (Strategy pattern)
        HealthScore healthScore = scoreCalculator.calculate(repoData);

        // 3. Alerts (Chain of Responsibility)
        List<Alert> alerts = alertEngine.evaluate(repoData);

        // 4. Language distribution (Enricher)
        LanguageDistribution languages = languageEnricher.enrich(repoData.getLanguages());

        // 5. Contributor activity
        ContributorActivity activity = buildContributorActivity(repoData);

        // 6. HATEOAS links
        var links = hateoasEnricher.buildLinks(repoData.getOwner(), repoData.getRepo());

        return AnalysisResponse.builder()
                .owner(repoData.getOwner())
                .repo(repoData.getRepo())
                .stats(stats)
                .healthScore(healthScore)
                .alerts(alerts)
                .languages(languages)
                .contributorActivity(activity)
                .source(source)
                .cachedAt(cachedAt)
                .links(links)
                .build();
    }

    /**
     * Builds contributor activity summary from raw contributor DTOs.
     */
    private ContributorActivity buildContributorActivity(RepoData repoData) {
        var contributors = repoData.getContributors();

        // Filter contributors with valid author info upfront
        List<GitHubContributorDTO> validContributors = contributors.stream()
                .filter(c -> c.getAuthor() != null)
                .collect(Collectors.toList());

        List<ContributorSummary> topContributors = validContributors.stream()
                .sorted((a, b) -> Integer.compare(b.getTotal(), a.getTotal()))
                .limit(TOP_CONTRIBUTORS_LIMIT)
                .map(c -> new ContributorSummary(
                        c.getAuthor().getLogin(),
                        c.getTotal(),
                        c.getActiveWeeks()))
                .collect(Collectors.toList());

        int totalCommits = validContributors.stream()
                .mapToInt(c -> {
                    if (c.getWeeks() != null) {
                        return c.getWeeks().stream().mapToInt(w -> w.getCommits()).sum();
                    }
                    return 0;
                })
                .sum();

        return ContributorActivity.builder()
                .totalContributors(validContributors.size())  // Use filtered count for consistency
                .topContributors(topContributors)
                .totalCommitsLast52Weeks(totalCommits)
                .lastCommitDate(repoData.getLastCommitDate())
                .build();
    }

    /**
     * Returns offline fallback: stale cache if available, otherwise mock.
     */
    private AnalysisResponse getOfflineFallback(String owner, String repo, String cacheKey) {
        Optional<AnalysisResponse> stale = cacheService.getStale(cacheKey);
        if (stale.isPresent()) {
            log.debug("Offline: returning stale cache for {}/{}", owner, repo);
            return withNotice(
                    withCacheMetadata(stale.get(), DataSource.STALE, stale.get().getCachedAt()),
                    "You are in offline mode. Showing previously cached data for this repository.");
        }
        log.debug("Offline: no cache for {}/{} — returning mock data", owner, repo);
        return withNotice(
                mockProvider.getMockData(owner, repo),
                "No saved data for " + owner + "/" + repo
                        + " in offline mode. This repository may not exist or was never analyzed online. Showing demo data.");
    }

    /**
     * Online failure fallback: serve real stale cache if available,
     * otherwise surface a clear error. We deliberately do NOT return demo/mock
     * data here — fabricated numbers must never be shown while the user is online.
     * Demo data is reserved for explicit offline mode.
     */
    private AnalysisResponse getStaleFallback(String owner, String repo, String cacheKey) {
        Optional<AnalysisResponse> stale = cacheService.getStale(cacheKey);
        if (stale.isPresent()) {
            log.info("API failure: returning STALE cache for {}/{}", owner, repo);
            return withNotice(
                    withCacheMetadata(stale.get(), DataSource.STALE, stale.get().getCachedAt()),
                    "GitHub is unreachable right now. Showing the last saved data for this repository (may be outdated).");
        }
        log.info("API failure + no cache for {}/{} — returning error (no demo data while online)", owner, repo);
        throw new GitHubApiException(
                "GitHub is currently unreachable and no cached data exists for this repository.",
                503, null);
    }

    /**
     * Returns a copy of the response with the given source and cachedAt overridden.
     * Used to correctly label CACHE vs STALE responses.
     */
    private AnalysisResponse withCacheMetadata(AnalysisResponse original,
                                                DataSource source,
                                                Instant cachedAt) {
        return AnalysisResponse.builder()
                .owner(original.getOwner())
                .repo(original.getRepo())
                .stats(original.getStats())
                .healthScore(original.getHealthScore())
                .alerts(original.getAlerts())
                .languages(original.getLanguages())
                .contributorActivity(original.getContributorActivity())
                .source(source)
                .cachedAt(cachedAt != null ? cachedAt : Instant.now())
                .notice(original.getNotice())
                .links(original.getLinks())
                .build();
    }

    /**
     * Returns a copy of the response with a user-facing notice attached.
     * Used to clearly explain why fallback/demo/stale data is being shown.
     */
    private AnalysisResponse withNotice(AnalysisResponse original, String notice) {
        return AnalysisResponse.builder()
                .owner(original.getOwner())
                .repo(original.getRepo())
                .stats(original.getStats())
                .healthScore(original.getHealthScore())
                .alerts(original.getAlerts())
                .languages(original.getLanguages())
                .contributorActivity(original.getContributorActivity())
                .source(original.getSource())
                .cachedAt(original.getCachedAt())
                .notice(notice)
                .links(original.getLinks())
                .build();
    }
}
