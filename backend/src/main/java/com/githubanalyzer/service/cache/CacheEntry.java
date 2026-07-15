package com.githubanalyzer.service.cache;

import com.githubanalyzer.domain.AnalysisResponse;

import java.time.Instant;

/**
 * Internal wrapper that adds TTL metadata to a cached {@link AnalysisResponse}.
 *
 * <p>Entries are stored in {@code InMemoryCacheService}'s map. The entry
 * tracks both the active TTL expiry ({@code expiresAt}) and whether the
 * entry is considered "stale" (past TTL but not yet evicted from the stale window).
 */
public class CacheEntry {

    private final AnalysisResponse data;
    private final Instant createdAt;
    private final Instant expiresAt;
    private volatile boolean stale;

    public CacheEntry(AnalysisResponse data, long ttlMillis) {
        this.data = data;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusMillis(ttlMillis);
        this.stale = false;
    }

    /**
     * Returns true if the entry has passed its active TTL.
     * Stale entries may still be served as a fallback.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Marks this entry as stale. Called when it expires but before eviction.
     */
    public void markStale() {
        this.stale = true;
    }

    public AnalysisResponse getData() { return data; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isStale() { return stale; }
}
