package com.githubanalyzer.controller;

import com.githubanalyzer.service.cache.CacheService;
import com.githubanalyzer.service.cache.CacheStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// SOLID - Single Responsibility: only handles cache management HTTP operations
// SOLID - Interface Segregation: depends only on CacheService, not the full AnalyzerService
@RestController
@RequestMapping("/api/cache")
@Tag(name = "Cache", description = "Cache management and statistics")
public class CacheController {

    private static final Logger log = LoggerFactory.getLogger(CacheController.class);

    // SOLID - Dependency Inversion: injected via interface, not concrete class
    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Operation(summary = "Get cache statistics")
    @GetMapping("/stats")
    public ResponseEntity<CacheStats> getStats() {
        return ResponseEntity.ok(cacheService.getStats());
    }

    @Operation(summary = "Clear entire cache")
    @DeleteMapping
    public ResponseEntity<Void> clearCache() {
        log.warn("Cache CLEAR requested via API");
        cacheService.clear();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Evict a specific repository from cache")
    @DeleteMapping("/{owner}/{repo}")
    public ResponseEntity<Void> evictEntry(@PathVariable String owner, @PathVariable String repo) {
        // key format matches what InMemoryCacheService uses internally
        String key = (owner + ":" + repo).toLowerCase();
        log.info("Cache EVICT requested for key: {}", key);
        cacheService.evict(key);
        return ResponseEntity.noContent().build();
    }
}
