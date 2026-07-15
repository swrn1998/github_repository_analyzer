package com.githubanalyzer.controller;

import com.githubanalyzer.domain.AnalysisRequest;
import com.githubanalyzer.domain.AnalysisResponse;
import com.githubanalyzer.domain.ComparisonResponse;
import com.githubanalyzer.service.AnalyzerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the repository analysis and comparison API.
 *
 * <p>Base path: {@code /api}
 *
 * <p>This controller is intentionally thin — it handles HTTP concerns only:
 * request mapping, parameter extraction, and response wrapping.
 * All business logic lives in {@link AnalyzerService}.
 *
 * <p>Offline mode is passed via the {@code X-Offline-Mode} request header.
 * When true, the service skips the live API and returns cached or mock data.
 */
@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Analysis", description = "Repository analysis and comparison operations")
public class AnalyzerController {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerController.class);

    private static final String GITHUB_NAME_PATTERN = "^[a-zA-Z0-9._-]{1,100}$";
    private static final String INVALID_NAME_MSG = "Must be alphanumeric with hyphens/underscores/dots, max 100 chars";

    private final AnalyzerService analyzerService;

    public AnalyzerController(AnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }

    // ── GET /api/analyze ─────────────────────────────────────────────────────

    @Operation(
            summary = "Analyze a GitHub repository",
            description = "Fetches repository data and returns enriched metrics including health score, alerts, and language distribution."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analysis successful",
                    content = @Content(schema = @Schema(implementation = AnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid owner or repo name"),
            @ApiResponse(responseCode = "404", description = "Repository not found on GitHub"),
            @ApiResponse(responseCode = "503", description = "GitHub API unavailable — stale or mock data returned")
    })
    @GetMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(
            @Parameter(description = "GitHub username or organization name", required = true, example = "facebook")
            @RequestParam
            @NotBlank(message = "owner must not be blank")
            @Pattern(regexp = GITHUB_NAME_PATTERN, message = INVALID_NAME_MSG)
            String owner,

            @Parameter(description = "Repository name", required = true, example = "react")
            @RequestParam
            @NotBlank(message = "repo must not be blank")
            @Pattern(regexp = GITHUB_NAME_PATTERN, message = INVALID_NAME_MSG)
            String repo,

            @Parameter(description = "When true, returns cached or mock data without calling GitHub API")
            @RequestHeader(value = "X-Offline-Mode", defaultValue = "false")
            boolean offlineMode) {

        log.info("GET /api/analyze?owner={}&repo={} (offline={})", owner, repo, offlineMode);

        AnalysisRequest request = new AnalysisRequest(owner, repo, offlineMode);
        AnalysisResponse response = analyzerService.analyze(request);

        return ResponseEntity.ok(response);
    }

    // ── GET /api/compare ─────────────────────────────────────────────────────

    @Operation(
            summary = "Compare two GitHub repositories",
            description = "Analyzes both repositories and returns a side-by-side comparison with a winner determination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comparison successful"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping("/compare")
    public ResponseEntity<ComparisonResponse> compare(
            @Parameter(required = true, example = "facebook") @RequestParam @NotBlank String ownerA,
            @Parameter(required = true, example = "react")    @RequestParam @NotBlank String repoA,
            @Parameter(required = true, example = "vuejs")    @RequestParam @NotBlank String ownerB,
            @Parameter(required = true, example = "vue")      @RequestParam @NotBlank String repoB,

            @RequestHeader(value = "X-Offline-Mode", defaultValue = "false")
            boolean offlineMode) {

        log.info("GET /api/compare?ownerA={}&repoA={}&ownerB={}&repoB={}",
                ownerA, repoA, ownerB, repoB);

        AnalysisRequest requestA = new AnalysisRequest(ownerA, repoA, offlineMode);
        AnalysisRequest requestB = new AnalysisRequest(ownerB, repoB, offlineMode);
        ComparisonResponse response = analyzerService.compare(requestA, requestB);

        return ResponseEntity.ok(response);
    }
}
