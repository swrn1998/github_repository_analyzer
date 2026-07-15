package com.githubanalyzer.service.cache;

import com.githubanalyzer.domain.AnalysisResponse;

import java.util.Optional;

/**
 * Contract for the in-memory analysis cache.
 *
 * <p>The cache provides two read modes:
 * <ul>
 *   <li>{@link #get} — returns only fresh (non-expired) entries</li>
 *   <li>{@link #getStale} — returns expired entries as a fallback
 *       when the GitHub API is unavailable</li>
 * </ul>
 *
 * <p>This enables the stale-while-revalidate degradation strategy:
 * serve old data rather than an error when the upstream is down.
 */
public interface CacheService {

    /**
     * Returns a cached response if present and not expired.
     *
     * @param key cache key (owner:repo, lowercase)
     * @return Optional.empty() on cache miss or expiry
     */
    Optional<AnalysisResponse> get(String key);

    /**
     * Returns a cached response even if expired.
     * Used as a fallback when the live API call fails.
     *
     * @param key cache key
     * @return Optional.empty() only if no entry exists (fresh or stale)
     */
    Optional<AnalysisResponse> getStale(String key);

    /**
     * Stores a response in the cache with the configured TTL.
     *
     * @param key   cache key
     * @param value the response to cache
     */
    void put(String key, AnalysisResponse value);

    /**
     * Removes a specific entry from the cache.
     */
    void evict(String key);

    /**
     * Clears all entries from the cache.
     */
    void clear();

    /**
     * Returns current cache statistics for monitoring.
     */
    CacheStats getStats();
}
