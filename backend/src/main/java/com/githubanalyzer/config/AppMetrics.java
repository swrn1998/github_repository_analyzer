package com.githubanalyzer.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Central registry of custom business metrics for the GitHub Repository Analyzer.
 *
 * <p>All application-specific Micrometer meters are defined here and injected
 * wherever needed. This follows the Single Responsibility Principle — metric
 * registration is not scattered across service classes.
 *
 * <p><b>Metrics exposed:</b>
 * <ul>
 *   <li>{@code analyzer_repo_analysis_total}        — total analyses (by source: LIVE/CACHE/STALE/MOCK)</li>
 *   <li>{@code analyzer_github_api_requests_total}   — GitHub API calls (by endpoint, outcome)</li>
 *   <li>{@code analyzer_github_api_latency_seconds}  — GitHub API call duration histogram</li>
 *   <li>{@code analyzer_cache_operations_total}      — cache hits / misses / stale-hits</li>
 *   <li>{@code analyzer_repo_comparison_total}       — comparison requests count</li>
 *   <li>{@code analyzer_alerts_triggered_total}      — alert type distribution</li>
 * </ul>
 */
@Component
public class AppMetrics {

    // ── Analysis counters ────────────────────────────────────────────────────
    private final Counter liveAnalysisCounter;
    private final Counter cacheAnalysisCounter;
    private final Counter staleAnalysisCounter;
    private final Counter mockAnalysisCounter;
    private final Counter comparisonCounter;

    // ── GitHub API metrics ────────────────────────────────────────────────
    private final Counter githubApiSuccessCounter;
    private final Counter githubApiFailureCounter;
    private final Counter githubApiRateLimitCounter;
    private final Timer   githubApiLatencyTimer;

    // ── Cache metrics ────────────────────────────────────────────────────────
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheStaleHitCounter;

    // ── Alert metrics ────────────────────────────────────────────────────────
    private final Counter inactiveRepoAlertCounter;
    private final Counter highIssueBacklogAlertCounter;
    private final Counter noTestInfraAlertCounter;
    private final Counter securityVulnAlertCounter;

    public AppMetrics(MeterRegistry registry) {

        // Analysis source counters
        liveAnalysisCounter = Counter.builder("analyzer.repo.analysis.total")
                .tag("source", "live")
                .description("Total repository analyses served from live GitHub API")
                .register(registry);

        cacheAnalysisCounter = Counter.builder("analyzer.repo.analysis.total")
                .tag("source", "cache")
                .description("Total repository analyses served from fresh cache")
                .register(registry);

        staleAnalysisCounter = Counter.builder("analyzer.repo.analysis.total")
                .tag("source", "stale")
                .description("Total repository analyses served from stale cache (API fallback)")
                .register(registry);

        mockAnalysisCounter = Counter.builder("analyzer.repo.analysis.total")
                .tag("source", "mock")
                .description("Total repository analyses served as mock data (offline mode)")
                .register(registry);

        comparisonCounter = Counter.builder("analyzer.repo.comparison.total")
                .description("Total repository comparison requests")
                .register(registry);

        // GitHub API counters
        githubApiSuccessCounter = Counter.builder("analyzer.github.api.requests.total")
                .tag("outcome", "success")
                .description("GitHub API calls that succeeded")
                .register(registry);

        githubApiFailureCounter = Counter.builder("analyzer.github.api.requests.total")
                .tag("outcome", "failure")
                .description("GitHub API calls that failed")
                .register(registry);

        githubApiRateLimitCounter = Counter.builder("analyzer.github.api.requests.total")
                .tag("outcome", "rate_limited")
                .description("GitHub API calls rejected due to rate limit")
                .register(registry);

        githubApiLatencyTimer = Timer.builder("analyzer.github.api.latency")
                .description("End-to-end GitHub API fetch latency (repo details + all supporting calls)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Cache operation counters
        cacheHitCounter = Counter.builder("analyzer.cache.operations.total")
                .tag("result", "hit")
                .description("Cache lookups that returned a fresh entry")
                .register(registry);

        cacheMissCounter = Counter.builder("analyzer.cache.operations.total")
                .tag("result", "miss")
                .description("Cache lookups that found no or expired entry")
                .register(registry);

        cacheStaleHitCounter = Counter.builder("analyzer.cache.operations.total")
                .tag("result", "stale_hit")
                .description("Cache lookups that returned a stale (expired) entry as fallback")
                .register(registry);

        // Alert counters
        inactiveRepoAlertCounter = Counter.builder("analyzer.alerts.triggered.total")
                .tag("type", "inactive_repo")
                .description("Number of inactive repository alerts triggered")
                .register(registry);

        highIssueBacklogAlertCounter = Counter.builder("analyzer.alerts.triggered.total")
                .tag("type", "high_issue_backlog")
                .description("Number of high issue backlog alerts triggered")
                .register(registry);

        noTestInfraAlertCounter = Counter.builder("analyzer.alerts.triggered.total")
                .tag("type", "no_test_infra")
                .description("Number of missing test infrastructure alerts triggered")
                .register(registry);

        securityVulnAlertCounter = Counter.builder("analyzer.alerts.triggered.total")
                .tag("type", "security_vulnerability")
                .description("Number of security vulnerability alerts triggered")
                .register(registry);
    }

    // ── Analysis ────────────────────────────────────────────────────────────
    public void recordLiveAnalysis()   { liveAnalysisCounter.increment(); }
    public void recordCacheAnalysis()  { cacheAnalysisCounter.increment(); }
    public void recordStaleAnalysis()  { staleAnalysisCounter.increment(); }
    public void recordMockAnalysis()   { mockAnalysisCounter.increment(); }
    public void recordComparison()     { comparisonCounter.increment(); }

    // ── GitHub API ──────────────────────────────────────────────────────────
    public void recordGitHubApiSuccess()   { githubApiSuccessCounter.increment(); }
    public void recordGitHubApiFailure()   { githubApiFailureCounter.increment(); }
    public void recordGitHubRateLimit()    { githubApiRateLimitCounter.increment(); }
    public Timer getGitHubApiLatencyTimer() { return githubApiLatencyTimer; }

    // ── Cache ───────────────────────────────────────────────────────────────
    public void recordCacheHit()       { cacheHitCounter.increment(); }
    public void recordCacheMiss()      { cacheMissCounter.increment(); }
    public void recordCacheStaleHit()  { cacheStaleHitCounter.increment(); }

    // ── Alerts ─────────────────────────────────────────────────────────────
    public void recordInactiveRepoAlert()       { inactiveRepoAlertCounter.increment(); }
    public void recordHighIssueBacklogAlert()    { highIssueBacklogAlertCounter.increment(); }
    public void recordNoTestInfraAlert()         { noTestInfraAlertCounter.increment(); }
    public void recordSecurityVulnAlert()        { securityVulnAlertCounter.increment(); }
}
