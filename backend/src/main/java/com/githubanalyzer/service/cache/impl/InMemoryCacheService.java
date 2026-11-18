package com.githubanalyzer.service.cache.impl;

import com.githubanalyzer.config.AppMetrics;
import com.githubanalyzer.domain.AnalysisResponse;
import com.githubanalyzer.service.cache.CacheEntry;
import com.githubanalyzer.service.cache.CacheService;
import com.githubanalyzer.service.cache.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// SOLID - Single Responsibility: manages only the in-memory TTL cache
// SOLID - Open/Closed: implements CacheService interface - could swap to Redis without changing callers
@Service
public class InMemoryCacheService implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCacheService.class);

    // ConcurrentHashMap for thread safety - multiple request threads read/write simultaneously
    private final ConcurrentHashMap<String, CacheEntry> store = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final long staleTtlMillis;
    private final int maxEntries;

    // AtomicLong for thread-safe counter increments without synchronization overhead
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong staleHits = new AtomicLong(0);

    private final AppMetrics appMetrics;

    // 12-Factor III (Config): all cache settings come from environment variables with defaults
    // e.g., CACHE_TTL_MINUTES=10 overrides the default without touching code
    // @Lazy on AppMetrics breaks a circular dependency during Spring context startup
    public InMemoryCacheService(
            @Value("${cache.ttl-minutes:10}") int ttlMinutes,
            @Value("${cache.stale-ttl-minutes:60}") int staleTtlMinutes,
            @Value("${cache.max-entries:500}") int maxEntries,
            @Lazy AppMetrics appMetrics) {
        this.ttlMillis = (long) ttlMinutes * 60 * 1000;
        this.staleTtlMillis = (long) staleTtlMinutes * 60 * 1000;
        this.maxEntries = maxEntries;
        this.appMetrics = appMetrics;
        log.info("Cache initialized: TTL={}min, stale-TTL={}min, maxEntries={}",
                ttlMinutes, staleTtlMinutes, maxEntries);
    }

    @Override
    public Optional<AnalysisResponse> get(String key) {
        CacheEntry entry = store.get(key);
        if (entry == null) {
            misses.incrementAndGet();
            appMetrics.recordCacheMiss();
            return Optional.empty();
        }
        if (entry.isExpired()) {
            // Mark stale but keep in store - getStale() can still serve it as fallback
            entry.markStale();
            misses.incrementAndGet();
            appMetrics.recordCacheMiss();
            return Optional.empty();
        }
        hits.incrementAndGet();
        appMetrics.recordCacheHit();
        return Optional.of(entry.getData());
    }

    @Override
    public Optional<AnalysisResponse> getStale(String key) {
        // Resilience (Option A) - stale-while-revalidate: serve old data rather than an error
        CacheEntry entry = store.get(key);
        if (entry == null) return Optional.empty();
        staleHits.incrementAndGet();
        appMetrics.recordCacheStaleHit();
        return Optional.of(entry.getData());
    }

    @Override
    public void put(String key, AnalysisResponse value) {
        if (store.size() >= maxEntries) evictOldest();
        store.put(key, new CacheEntry(value, ttlMillis));
    }

    @Override
    public void evict(String key) {
        store.remove(key);
    }

    @Override
    public void clear() {
        int size = store.size();
        store.clear();
        log.info("Cache cleared: {} entries removed", size);
    }

    @Override
    public CacheStats getStats() {
        return new CacheStats(store.size(), hits.get(), misses.get(), staleHits.get());
    }

    // @Scheduled requires @EnableScheduling in GithubAnalyzerApplication to work
    // Runs every N minutes (configurable) to remove entries past their stale TTL window
    // fixedRateString reads from property - if property is "5", the value becomes "5000" (ms)
    @Scheduled(fixedRateString = "${cache.eviction-scheduler-minutes:5}000")
    public void evictExpiredEntries() {
        int[] evicted = {0};
        store.entrySet().removeIf(entry -> {
            long ageMillis = Duration.between(entry.getValue().getCreatedAt(), Instant.now()).toMillis();
            if (ageMillis > (ttlMillis + staleTtlMillis)) {
                evicted[0]++;
                return true;
            }
            return false;
        });
        if (evicted[0] > 0) {
            log.info("Cache eviction: removed {} expired entries. Current size: {}", evicted[0], store.size());
        }
    }

    // Evicts oldest 20% of entries when cache is at capacity (memory pressure safeguard)
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
