package com.githubanalyzer.service.cache;

/**
 * Snapshot of cache performance metrics.
 * Exposed via {@code GET /api/cache/stats}.
 */
public class CacheStats {

    private final int totalEntries;
    private final long hits;
    private final long misses;
    private final long staleHits;

    public CacheStats(int totalEntries, long hits, long misses, long staleHits) {
        this.totalEntries = totalEntries;
        this.hits = hits;
        this.misses = misses;
        this.staleHits = staleHits;
    }

    /**
     * Returns the cache hit rate as a double between 0.0 and 1.0.
     */
    public double getHitRate() {
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }

    public int getTotalEntries() { return totalEntries; }
    public long getHits() { return hits; }
    public long getMisses() { return misses; }
    public long getStaleHits() { return staleHits; }
}
