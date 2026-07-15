package com.githubanalyzer.service.cache.impl;

import com.githubanalyzer.domain.AnalysisResponse;
import com.githubanalyzer.service.cache.CacheEntry;
import com.githubanalyzer.service.cache.CacheService;
import com.githubanalyzer.service.cache.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe in-memory TTL cache backed by {@link ConcurrentHashMap}.
 *
 * <p><b>Degradation Strategy (Option A):</b>
 * <ol>
 *   <li>{@link #get}: Returns fresh data within TTL window</li>
 *   <li>{@link #getStale}: Returns expired data when live API fails</li>
 *   <li>If no stale data: caller falls back to MockDataProvider</li>
 * </ol>
 *
 * <p>A scheduled task runs every 5 minutes to evict entries that have
 * exceeded the stale TTL window (active TTL + stale grace period).
 *
 * <p>If the map exceeds {@code maxEntries}, the oldest 20% of entries
 * are evicted to prevent unbounded memory growth.
 *
 * <p>All counters use {@link AtomicLong} for lock-free concurrent updates.
 */
@Service
public class InMemoryCacheService implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCacheService.class);

    private final ConcurrentHashMap<String, CacheEntry> store = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final long staleTtlMillis;
    private final int maxEntries;

    // ── Metrics counters ─────────────────────────────────────────────────────
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong staleHits = new AtomicLong(0);

    public InMemoryCacheService(
            @Value("${cache.ttl-minutes:10}") int ttlMinutes,
            @Value("${cache.stale-ttl-minutes:60}") int staleTtlMinutes,
            @Value("${cache.max-entries:500}") int maxEntries) {
        this.ttlMillis = (long) ttlMinutes * 60 * 1000;
        this.staleTtlMillis = (long) staleTtlMinutes * 60 * 1000;
        this.maxEntries = maxEntries;
        log.info("Cache initialized: TTL={}min, stale-TTL={}min, maxEntries={}",
                ttlMinutes, staleTtlMinutes, maxEntries);
    }

    // ── CacheService implementation ──────────────────────────────────────────

    @Override
    public Optional<AnalysisResponse> get(String key) {
        CacheEntry entry = store.get(key);

        if (entry == null) {
            misses.incrementAndGet();
            log.debug("Cache MISS for key: {}", key);
            return Optional.empty();
        }

        if (entry.isExpired()) {
            entry.markStale();
            misses.incrementAndGet();
            log.debug("Cache EXPIRED for key: {} (created: {})", key, entry.getCreatedAt());
            return Optional.empty();
        }

        hits.incrementAndGet();
        log.debug("Cache HIT for key: {}", key);
        return Optional.of(entry.getData());
    }

    @Override
    public Optional<AnalysisResponse> getStale(String key) {
        CacheEntry entry = store.get(key);

        if (entry == null) {
            log.debug("No stale entry found for key: {}", key);
            return Optional.empty();
        }

        staleHits.incrementAndGet();
        log.debug("Cache STALE HIT for key: {} (created: {})", key, entry.getCreatedAt());
        return Optional.of(entry.getData());
    }

    @Override
    public void put(String key, AnalysisResponse value) {
        if (store.size() >= maxEntries) {
            evictOldest();
        }
        store.put(key, new CacheEntry(value, ttlMillis));
        log.debug("Cache PUT for key: {} (TTL: {}ms)", key, ttlMillis);
    }

    @Override
    public void evict(String key) {
        store.remove(key);
        log.debug("Cache EVICT for key: {}", key);
    }

    @Override
    public void clear() {
        int size = store.size();
        store.clear();
        log.info("Cache CLEARED: {} entries removed", size);
    }

    @Override
    public CacheStats getStats() {
        return new CacheStats(store.size(), hits.get(), misses.get(), staleHits.get());
    }

    // ── Scheduled eviction ───────────────────────────────────────────────────

    /**
     * Runs every 5 minutes to evict entries that have exceeded the stale window.
     * Entries past (TTL + stale-TTL) are no longer useful and should be freed.
     */
    @Scheduled(fixedRateString = "${cache.eviction-scheduler-minutes:5}000")
    public void evictExpiredEntries() {
        long staleCutoffMillis = staleTtlMillis;
        int[] evicted = {0};

        store.entrySet().removeIf(entry -> {
            long ageMillis = java.time.Duration.between(
                    entry.getValue().getCreatedAt(),
                    java.time.Instant.now()
            ).toMillis();

            if (ageMillis > (ttlMillis + staleCutoffMillis)) {
                evicted[0]++;
                return true;
            }
            return false;
        });

        if (evicted[0] > 0) {
            log.info("Cache eviction: removed {} expired entries. Current size: {}",
                    evicted[0], store.size());
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Evicts the oldest 20% of entries when the cache is full.
     * Uses creation time as the LRU approximation.
     */
    private void evictOldest() {
        int toEvict = Math.max(1, maxEntries / 5);
        log.warn("Cache full ({}). Evicting {} oldest entries.", store.size(), toEvict);

        store.entrySet().stream()
                .sorted(java.util.Comparator.comparing(e -> e.getValue().getCreatedAt()))
                .limit(toEvict)
                .map(java.util.Map.Entry::getKey)
                .forEach(store::remove);
    }
}
