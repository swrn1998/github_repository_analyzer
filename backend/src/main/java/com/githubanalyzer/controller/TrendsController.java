package com.githubanalyzer.controller;

import com.githubanalyzer.domain.TrendsResponse;
import com.githubanalyzer.service.TrendsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for historical trend data.
 *
 * <p>Provides endpoints to view repository metrics over time.
 */
@RestController
@RequestMapping("/api/trends")
@CrossOrigin(origins = "*")
public class TrendsController {

    private static final Logger log = LoggerFactory.getLogger(TrendsController.class);

    private final TrendsService trendsService;

    public TrendsController(TrendsService trendsService) {
        this.trendsService = trendsService;
    }

    /**
     * Get historical trend data for a repository.
     *
     * @param owner repository owner
     * @param repo repository name
     * @return trend snapshots and summary
     */
    @GetMapping
    public ResponseEntity<TrendsResponse> getTrends(
            @RequestParam String owner,
            @RequestParam String repo) {
        
        log.info("GET /api/trends?owner={}&repo={}", owner, repo);
        
        TrendsResponse response = trendsService.getTrends(owner, repo);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get total snapshot count across all repositories (diagnostic endpoint).
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        int totalSnapshots = trendsService.getTotalSnapshotCount();
        
        return ResponseEntity.ok(Map.of(
                "totalSnapshots", totalSnapshots,
                "message", "Total trend snapshots stored in memory"
        ));
    }

    /**
     * Clear all trend data (admin/testing endpoint).
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearAll() {
        log.warn("DELETE /api/trends - Clearing all trend data");
        
        trendsService.clearAll();
        
        return ResponseEntity.ok(Map.of(
                "message", "All trend data cleared successfully"
        ));
    }
}
