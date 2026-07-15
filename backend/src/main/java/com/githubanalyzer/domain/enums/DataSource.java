package com.githubanalyzer.domain.enums;

/**
 * Indicates the origin and freshness of the data in an AnalysisResponse.
 *
 * <p>This is surfaced to the UI so users know how much to trust the data.
 * The degradation order is: LIVE → CACHE → STALE → MOCK
 */
public enum DataSource {

    /**
     * Freshly fetched from the GitHub REST API.
     * Most trustworthy, subject to GitHub API availability.
     */
    LIVE,

    /**
     * Returned from in-memory cache within the TTL window.
     * Fresh enough for most use cases.
     */
    CACHE,

    /**
     * Returned from an expired cache entry because GitHub API was unavailable.
     * Data may be outdated — UI should show a warning banner.
     */
    STALE,

    /**
     * Static mock/seed data returned when no cache exists and API is unavailable,
     * or when the user explicitly enables offline mode.
     * For demonstration purposes only.
     */
    MOCK
}
