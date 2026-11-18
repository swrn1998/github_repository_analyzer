package com.githubanalyzer.controller;

import com.githubanalyzer.domain.AnalysisRequest;
import com.githubanalyzer.domain.AnalysisResponse;
import com.githubanalyzer.domain.ComparisonResponse;
import com.githubanalyzer.service.AnalyzerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

// SOLID - Single Responsibility: this class only handles HTTP concerns
// (request mapping, input validation, response wrapping)
// All business logic is delegated to AnalyzerService
@RestController
@RequestMapping("/api")
@Validated  // activates bean validation on @RequestParam fields (e.g., @NotBlank, @Pattern)
@Tag(name = "Analysis", description = "Repository analysis and comparison")
public class AnalyzerController {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerController.class);
    private static final String GITHUB_NAME_PATTERN = "^[a-zA-Z0-9._-]{1,100}$";
    private static final String INVALID_NAME_MSG = "Must be alphanumeric with hyphens/underscores/dots, max 100 chars";

    // SOLID - Dependency Inversion: depends on the AnalyzerService interface, not the implementation
    // Spring injects the actual impl (AnalyzerServiceImpl) at runtime via constructor injection
    private final AnalyzerService analyzerService;

    public AnalyzerController(AnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }

    @Operation(summary = "Analyze a GitHub repository")
    @GetMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(
            // @NotBlank + @Pattern = input validation at the HTTP layer, before hitting any business logic
            @RequestParam @NotBlank(message = "owner must not be blank")
            @Pattern(regexp = GITHUB_NAME_PATTERN, message = INVALID_NAME_MSG) String owner,

            @RequestParam @NotBlank(message = "repo must not be blank")
            @Pattern(regexp = GITHUB_NAME_PATTERN, message = INVALID_NAME_MSG) String repo,

            // X-Offline-Mode header lets the frontend toggle demo/cache-only mode without changing the URL
            @RequestHeader(value = "X-Offline-Mode", defaultValue = "false") boolean offlineMode) {

        long startTime = System.currentTimeMillis();
        log.info(">>> REQUEST START  GET /api/analyze | owner={} repo={} offline={}", owner, repo, offlineMode);

        AnalysisResponse result = analyzerService.analyze(new AnalysisRequest(owner, repo, offlineMode));

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("<<< REQUEST END    GET /api/analyze | owner={} repo={} source={} elapsed={}ms",
                owner, repo, result.getSource(), elapsed);

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Compare two GitHub repositories")
    @GetMapping("/compare")
    public ResponseEntity<ComparisonResponse> compare(
            @RequestParam @NotBlank String ownerA,
            @RequestParam @NotBlank String repoA,
            @RequestParam @NotBlank String ownerB,
            @RequestParam @NotBlank String repoB,
            @RequestHeader(value = "X-Offline-Mode", defaultValue = "false") boolean offlineMode) {

        long startTime = System.currentTimeMillis();
        log.info(">>> REQUEST START  GET /api/compare | repoA={}/{} repoB={}/{} offline={}",
                ownerA, repoA, ownerB, repoB, offlineMode);

        ComparisonResponse result = analyzerService.compare(
                new AnalysisRequest(ownerA, repoA, offlineMode),
                new AnalysisRequest(ownerB, repoB, offlineMode)
        );

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("<<< REQUEST END    GET /api/compare | repoA={}/{} repoB={}/{} elapsed={}ms",
                ownerA, repoA, ownerB, repoB, elapsed);

        return ResponseEntity.ok(result);
    }
}
