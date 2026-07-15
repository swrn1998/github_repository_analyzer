package com.githubanalyzer.controller;

import com.githubanalyzer.service.cache.CacheService;
import com.githubanalyzer.service.cache.CacheStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for cache management and monitoring.
 *
 * <p>Provides visibility into cache performance (hit/miss rates)
 * and manual cache invalidation for operational needs.
 */
@RestController
@RequestMapping("/api/cache")
@Tag(name = "Cache", description = "Cache management and statistics")
public class CacheController {

    private static final Logger log = LoggerFactory.getLogger(CacheController.class);

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Operation(summary = "Get cache statistics",
            description = "Returns hit/miss rates and current entry count.")
    @GetMapping("/stats")
    public ResponseEntity<CacheStats> getStats() {
        return ResponseEntity.ok(cacheService.getStats());
    }

    @Operation(summary = "Clear entire cache",
            description = "Removes all cached entries. Use when you need fresh data for all repos.")
    @DeleteMapping
    public ResponseEntity<Void> clearCache() {
        log.warn("Cache CLEAR requested via API");
        cacheService.clear();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Evict a specific repository from cache")
    @DeleteMapping("/{owner}/{repo}")
    public ResponseEntity<Void> evictEntry(
            @PathVariable String owner,
            @PathVariable String repo) {
        String key = (owner + ":" + repo).toLowerCase();
        log.info("Cache EVICT requested for key: {}", key);
        cacheService.evict(key);
        return ResponseEntity.noContent().build();
    }
}
