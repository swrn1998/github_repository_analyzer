package com.githubanalyzer.service;

import com.githubanalyzer.domain.AnalysisRequest;
import com.githubanalyzer.domain.AnalysisResponse;
import com.githubanalyzer.domain.ComparisonResponse;

/**
 * Primary service contract for the GitHub Repository Analyzer.
 *
 * <p>This is the Facade that hides all the complexity of:
 * cache lookups, GitHub API calls, health scoring, alert evaluation,
 * language enrichment, and HATEOAS link building.
 *
 * <p>The controller only calls this service and knows nothing about
 * how data is fetched or computed.
 */
public interface AnalyzerService {

    /**
     * Analyzes a GitHub repository and returns enriched metrics.
     *
     * <p>Degradation order:
     * <ol>
     *   <li>Cache hit (fresh) → return immediately</li>
     *   <li>Offline mode → return cache or mock</li>
     *   <li>Live GitHub API → enrich, cache, return</li>
     *   <li>API failure → return stale cache</li>
     *   <li>No stale → return mock data</li>
     * </ol>
     *
     * @param request analysis parameters (owner, repo, offlineMode)
     * @return enriched analysis response — never null, always has a valid source
     */
    AnalysisResponse analyze(AnalysisRequest request);

    /**
     * Compares two GitHub repositories and returns a side-by-side result.
     *
     * <p>Internally calls {@link #analyze} for each repository (possibly in parallel).
     *
     * @param requestA first repository
     * @param requestB second repository
     * @return comparison result with winner determination
     */
    ComparisonResponse compare(AnalysisRequest requestA, AnalysisRequest requestB);
}
