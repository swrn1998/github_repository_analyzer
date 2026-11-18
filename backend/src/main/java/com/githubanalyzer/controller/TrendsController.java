package com.githubanalyzer.controller;

import com.githubanalyzer.domain.TrendsResponse;
import com.githubanalyzer.service.TrendsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// SOLID - Single Responsibility: handles only trend data HTTP endpoints
@RestController
// Two mappings handle the case where nginx or the frontend accidentally double-prefixes /api
@RequestMapping({"/api/trends", "/api/api/trends"})
@CrossOrigin(origins = "*")  // allows the React frontend to call this from a different domain
public class TrendsController {

    private static final Logger log = LoggerFactory.getLogger(TrendsController.class);

    // SOLID - Dependency Inversion: depends on TrendsService interface
    private final TrendsService trendsService;

    public TrendsController(TrendsService trendsService) {
        this.trendsService = trendsService;
    }

    @GetMapping
    public ResponseEntity<TrendsResponse> getTrends(
            @RequestParam String owner,
            @RequestParam String repo) {
        log.info("GET /api/trends?owner={}&repo={}", owner, repo);
        return ResponseEntity.ok(trendsService.getTrends(owner, repo));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "totalSnapshots", trendsService.getTotalSnapshotCount(),
                "message", "Total trend snapshots stored in memory"
        ));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearAll() {
        log.warn("DELETE /api/trends - Clearing all trend data");
        trendsService.clearAll();
        return ResponseEntity.ok(Map.of("message", "All trend data cleared successfully"));
    }
}
