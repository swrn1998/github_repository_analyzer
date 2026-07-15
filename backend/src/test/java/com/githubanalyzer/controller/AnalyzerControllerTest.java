package com.githubanalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.githubanalyzer.controller.advice.GlobalExceptionHandler;
import com.githubanalyzer.domain.*;
import com.githubanalyzer.domain.enums.AlertType;
import com.githubanalyzer.domain.enums.DataSource;
import com.githubanalyzer.domain.enums.Severity;
import com.githubanalyzer.exception.GitHubApiException;
import com.githubanalyzer.exception.RateLimitExceededException;
import com.githubanalyzer.exception.RepoNotFoundException;
import com.githubanalyzer.service.AnalyzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link AnalyzerController} using {@code @WebMvcTest}.
 *
 * <p>{@code @WebMvcTest} spins up only the web layer (controller + filters +
 * exception handlers). The {@link AnalyzerService} is mocked with
 * {@code @MockBean} — no database, no Spring context, no GitHub API calls.
 *
 * <p>Tests are grouped by concern:
 * <ol>
 *   <li>{@code GET /api/analyze} — happy path</li>
 *   <li>{@code GET /api/analyze} — input validation errors</li>
 *   <li>{@code GET /api/analyze} — offline mode header behaviour</li>
 *   <li>{@code GET /api/analyze} — exception-to-HTTP mapping (404, 503, 429, 500)</li>
 *   <li>{@code GET /api/analyze} — response body structure assertions</li>
 *   <li>{@code GET /api/compare} — happy path</li>
 *   <li>{@code GET /api/compare} — validation & exception propagation</li>
 *   <li>Cross-cutting: CORS headers, Content-Type, missing parameters</li>
 * </ol>
 *
 * <p>The {@link GlobalExceptionHandler} is pulled in via {@code @Import} so
 * exception-to-HTTP mapping is tested end-to-end through the real handler,
 * not mocked.
 */
@WebMvcTest(AnalyzerController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AnalyzerController")
class AnalyzerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyzerService analyzerService;

    // ── Shared test fixtures ─────────────────────────────────────────────────

    private AnalysisResponse liveResponse;
    private AnalysisResponse staleResponse;
    private AnalysisResponse mockResponse;

    @BeforeEach
    void setUpFixtures() {
        liveResponse  = buildAnalysisResponse("facebook", "react",  DataSource.LIVE,  null);
        staleResponse = buildAnalysisResponse("facebook", "react",  DataSource.STALE, Instant.parse("2026-06-15T08:00:00Z"));
        mockResponse  = buildAnalysisResponse("facebook", "react",  DataSource.MOCK,  Instant.now());
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/analyze — Happy path
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/analyze — happy path")
    class AnalyzeHappyPath {

        @Test
        @DisplayName("returns 200 and JSON body for valid owner and repo")
        void returns200_forValidOwnerAndRepo() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("returns owner and repo in the response body")
        void responseBody_containsOwnerAndRepo() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.owner").value("facebook"))
                    .andExpect(jsonPath("$.repo").value("react"));
        }

        @Test
        @DisplayName("returns source=LIVE for a fresh response")
        void responseBody_containsSourceLive() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.source").value("LIVE"));
        }

        @Test
        @DisplayName("returns healthScore with score and grade")
        void responseBody_containsHealthScore() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.healthScore.score").value(88))
                    .andExpect(jsonPath("$.healthScore.grade").value("B"));
        }

        @Test
        @DisplayName("returns healthScore.breakdown with all four dimensions")
        void responseBody_containsScoreBreakdown() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.healthScore.breakdown.commitActivityScore").value(25))
                    .andExpect(jsonPath("$.healthScore.breakdown.issueRatioScore").value(18))
                    .andExpect(jsonPath("$.healthScore.breakdown.communityScore").value(25))
                    .andExpect(jsonPath("$.healthScore.breakdown.documentationScore").value(20));
        }

        @Test
        @DisplayName("returns alerts array in response body")
        void responseBody_containsAlerts() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.alerts").isArray())
                    .andExpect(jsonPath("$.alerts", hasSize(1)))
                    .andExpect(jsonPath("$.alerts[0].type").value("HIGH_ISSUE_BACKLOG"))
                    .andExpect(jsonPath("$.alerts[0].severity").value("WARNING"))
                    .andExpect(jsonPath("$.alerts[0].message").value("High issue backlog - Maintenance concern"));
        }

        @Test
        @DisplayName("returns empty alerts array when no alerts are raised")
        void responseBody_containsEmptyAlerts_whenNoneRaised() throws Exception {
            AnalysisResponse noAlerts = buildAnalysisResponse("torvalds", "linux", DataSource.LIVE, null);
            when(analyzerService.analyze(any())).thenReturn(noAlerts);

            perform("/api/analyze?owner=torvalds&repo=linux")
                    .andExpect(jsonPath("$.alerts").isArray())
                    .andExpect(jsonPath("$.alerts").isEmpty());
        }

        @Test
        @DisplayName("returns stats with numeric values")
        void responseBody_containsStats() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.stats.stars").value(220000))
                    .andExpect(jsonPath("$.stats.forks").value(45000))
                    .andExpect(jsonPath("$.stats.openIssues").value(854));
        }

        @Test
        @DisplayName("returns HATEOAS _links object in response body")
        void responseBody_containsHateoasLinks() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$._links").exists())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.github.href").exists());
        }

        @Test
        @DisplayName("delegates to analyzerService.analyze() exactly once")
        void delegatesToService_exactlyOnce() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react");

            verify(analyzerService, times(1)).analyze(any());
            verifyNoMoreInteractions(analyzerService);
        }

        @Test
        @DisplayName("passes correct owner and repo to service")
        void passesOwnerAndRepo_toService() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=microsoft&repo=vscode");

            verify(analyzerService).analyze(argThat(req ->
                    "microsoft".equals(req.getOwner()) &&
                    "vscode".equals(req.getRepo())
            ));
        }

        @Test
        @DisplayName("passes offlineMode=false to service when header is absent")
        void passesOfflineModeFalse_whenHeaderAbsent() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react");

            verify(analyzerService).analyze(argThat(req -> !req.isOfflineMode()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/analyze — Input validation
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/analyze — input validation")
    class AnalyzeValidation {

        @Test
        @DisplayName("returns 400 when 'owner' parameter is missing entirely")
        void returns400_whenOwnerParamIsMissing() throws Exception {
            perform("/api/analyze?repo=react")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                    .andExpect(jsonPath("$.message").value(containsString("owner")));
        }

        @Test
        @DisplayName("returns 400 when 'repo' parameter is missing entirely")
        void returns400_whenRepoParamIsMissing() throws Exception {
            perform("/api/analyze?owner=facebook")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                    .andExpect(jsonPath("$.message").value(containsString("repo")));
        }

        @Test
        @DisplayName("returns 400 when both parameters are missing")
        void returns400_whenBothParamsMissing() throws Exception {
            perform("/api/analyze")
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest(name = "owner=''{0}'' → 400 INVALID_INPUT")
        @ValueSource(strings = {
            "owner with spaces",      // spaces not allowed
            "owner/with/slashes",     // slashes not allowed
            "owner@domain",           // @ not allowed
            "owner#hash",             // # not allowed
            "owner!bang",             // ! not allowed
            "<script>",               // injection attempt
            "../../../../etc/passwd", // path traversal attempt
        })
        @DisplayName("returns 400 for owner names with invalid characters")
        void returns400_forOwnerWithInvalidCharacters(String invalidOwner) throws Exception {
            mockMvc.perform(get("/api/analyze")
                            .param("owner", invalidOwner)
                            .param("repo", "react"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @ParameterizedTest(name = "repo=''{0}'' → 400 INVALID_INPUT")
        @ValueSource(strings = {
            "repo with spaces",
            "repo/subpath",
            "repo@version",
            "<script>alert(1)</script>",
        })
        @DisplayName("returns 400 for repo names with invalid characters")
        void returns400_forRepoWithInvalidCharacters(String invalidRepo) throws Exception {
            mockMvc.perform(get("/api/analyze")
                            .param("owner", "facebook")
                            .param("repo", invalidRepo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("returns 400 for owner name longer than 100 characters")
        void returns400_forOwnerExceeding100Chars() throws Exception {
            String tooLong = "a".repeat(101);
            mockMvc.perform(get("/api/analyze")
                            .param("owner", tooLong)
                            .param("repo", "react"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("returns 400 for repo name longer than 100 characters")
        void returns400_forRepoExceeding100Chars() throws Exception {
            String tooLong = "r".repeat(101);
            mockMvc.perform(get("/api/analyze")
                            .param("owner", "facebook")
                            .param("repo", tooLong))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("error response contains 'status' field equal to 400")
        void errorResponse_containsStatusField() throws Exception {
            perform("/api/analyze?repo=react")
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("error response contains a 'timestamp' field in ISO-8601 format")
        void errorResponse_containsTimestamp() throws Exception {
            perform("/api/analyze?repo=react")
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.timestamp").isString());
        }

        @Test
        @DisplayName("error response contains a 'path' field matching the request URI")
        void errorResponse_containsPath() throws Exception {
            perform("/api/analyze?repo=react")
                    .andExpect(jsonPath("$.path").value(containsString("/api/analyze")));
        }

        @ParameterizedTest(name = "valid owner=''{0}'' is accepted")
        @ValueSource(strings = {
            "facebook",
            "my-org",
            "user.name",
            "user_name",
            "User123",
            "a",                        // single character
            "org-with-dashes-and.dots",
        })
        @DisplayName("returns 200 for valid GitHub-format owner names")
        void returns200_forValidOwnerNames(String validOwner) throws Exception {
            when(analyzerService.analyze(any())).thenReturn(
                    buildAnalysisResponse(validOwner, "repo", DataSource.LIVE, null));

            mockMvc.perform(get("/api/analyze")
                            .param("owner", validOwner)
                            .param("repo", "repo"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("service is NOT called when validation fails")
        void serviceNotCalled_whenValidationFails() throws Exception {
            perform("/api/analyze?owner=invalid owner&repo=react");

            verify(analyzerService, never()).analyze(any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/analyze — Offline mode header
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/analyze — X-Offline-Mode header")
    class OfflineModeHeader {

        @Test
        @DisplayName("passes offlineMode=true to service when X-Offline-Mode: true header is set")
        void passesOfflineModeTrue_whenHeaderIsTrue() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(mockResponse);

            mockMvc.perform(get("/api/analyze")
                            .param("owner", "facebook")
                            .param("repo", "react")
                            .header("X-Offline-Mode", "true"))
                    .andExpect(status().isOk());

            verify(analyzerService).analyze(argThat(req -> req.isOfflineMode()));
        }

        @Test
        @DisplayName("passes offlineMode=false to service when X-Offline-Mode: false is set explicitly")
        void passesOfflineModeFalse_whenHeaderIsFalse() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            mockMvc.perform(get("/api/analyze")
                            .param("owner", "facebook")
                            .param("repo", "react")
                            .header("X-Offline-Mode", "false"))
                    .andExpect(status().isOk());

            verify(analyzerService).analyze(argThat(req -> !req.isOfflineMode()));
        }

        @Test
        @DisplayName("defaults to offlineMode=false when X-Offline-Mode header is absent")
        void defaultsToFalse_whenHeaderAbsent() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            mockMvc.perform(get("/api/analyze")
                            .param("owner", "facebook")
                            .param("repo", "react"))
                    .andExpect(status().isOk());

            verify(analyzerService).analyze(argThat(req -> !req.isOfflineMode()));
        }

        @Test
        @DisplayName("returns source=MOCK in body when offline mode returns mock data")
        void responseBody_showsMockSource_whenOffline() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(mockResponse);

            mockMvc.perform(get("/api/analyze")
                            .param("owner", "facebook")
                            .param("repo", "react")
                            .header("X-Offline-Mode", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.source").value("MOCK"));
        }

        @Test
        @DisplayName("returns source=STALE in body when API failed and stale cache is served")
        void responseBody_showsStaleSource_whenApiUnavailable() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(staleResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.source").value("STALE"));
        }

        @Test
        @DisplayName("returns cachedAt field when source is STALE")
        void responseBody_containsCachedAt_whenStale() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(staleResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.cachedAt").value(notNullValue()));
        }

        @Test
        @DisplayName("cachedAt is null in body when source is LIVE")
        void responseBody_cachedAtIsNull_whenLive() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.cachedAt").doesNotExist());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/analyze — Exception → HTTP status mapping
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/analyze — exception to HTTP status mapping")
    class AnalyzeExceptionMapping {

        // ── 404 RepoNotFoundException ────────────────────────────────────────

        @Test
        @DisplayName("returns 404 when RepoNotFoundException is thrown by service")
        void returns404_whenRepoNotFound() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RepoNotFoundException("facebook", "nonexistent"));

            perform("/api/analyze?owner=facebook&repo=nonexistent")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("404 response has error code REPO_NOT_FOUND")
        void notFound_responseHasCorrectCode() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RepoNotFoundException("facebook", "nonexistent"));

            perform("/api/analyze?owner=facebook&repo=nonexistent")
                    .andExpect(jsonPath("$.code").value("REPO_NOT_FOUND"));
        }

        @Test
        @DisplayName("404 response message includes the owner/repo in the error message")
        void notFound_responseMessageContainsRepoName() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RepoNotFoundException("facebook", "nonexistent"));

            perform("/api/analyze?owner=facebook&repo=nonexistent")
                    .andExpect(jsonPath("$.message").value(
                            containsString("nonexistent")));
        }

        @Test
        @DisplayName("404 response has status=404 in the body")
        void notFound_responseBodyStatus() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RepoNotFoundException("facebook", "nonexistent"));

            perform("/api/analyze?owner=facebook&repo=nonexistent")
                    .andExpect(jsonPath("$.status").value(404));
        }

        // ── 503 GitHubApiException ────────────────────────────────────────────

        @Test
        @DisplayName("returns 503 when GitHubApiException is thrown by service")
        void returns503_whenGitHubApiExceptionThrown() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new GitHubApiException("GitHub API unavailable", 503, null));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("503 response has error code GITHUB_API_ERROR")
        void serviceUnavailable_responseHasCorrectCode() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new GitHubApiException("GitHub API unavailable", 503, null));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.code").value("GITHUB_API_ERROR"));
        }

        @Test
        @DisplayName("503 response message does NOT leak internal exception details")
        void serviceUnavailable_doesNotLeakInternalDetails() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new GitHubApiException("Internal details should not appear", 503, "raw-body"));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.message")
                            .value(not(containsString("Internal details should not appear"))));
        }

        @Test
        @DisplayName("503 response message is a safe user-facing message")
        void serviceUnavailable_messageIsUserFacing() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new GitHubApiException("GitHub API unavailable", 503, null));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.message")
                            .value(containsString("unavailable")));
        }

        @Test
        @DisplayName("returns 503 for GitHubApiException with connection timeout (status -1)")
        void returns503_forConnectionTimeoutException() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new GitHubApiException("Connection timed out",
                            new RuntimeException("Read timeout")));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("GITHUB_API_ERROR"));
        }

        // ── 429 RateLimitExceededException ────────────────────────────────────

        @Test
        @DisplayName("returns 429 when RateLimitExceededException is thrown")
        void returns429_whenRateLimitExceeded() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RateLimitExceededException(1750000000L));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("429 response has error code RATE_LIMIT_EXCEEDED")
        void rateLimited_responseHasCorrectCode() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RateLimitExceededException(1750000000L));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
        }

        @Test
        @DisplayName("429 response advises the user to retry later")
        void rateLimited_messageAdvisesRetry() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RateLimitExceededException(1750000000L));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.message").value(containsString("try again later")));
        }

        // ── 500 generic exception ─────────────────────────────────────────────

        @Test
        @DisplayName("returns 500 for unexpected RuntimeException")
        void returns500_forUnexpectedRuntimeException() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RuntimeException("Something went very wrong"));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("500 response has error code INTERNAL_ERROR")
        void internalError_responseHasCorrectCode() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RuntimeException("Something went very wrong"));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        }

        @Test
        @DisplayName("500 response does NOT leak exception message to client")
        void internalError_doesNotLeakExceptionMessage() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RuntimeException("Sensitive internal detail"));

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.message")
                            .value(not(containsString("Sensitive internal detail"))));
        }

        // ── Shared error response structure ───────────────────────────────────

        @Test
        @DisplayName("all error responses contain timestamp, status, code, message, path fields")
        void allErrorResponses_containRequiredFields() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RepoNotFoundException("owner", "repo"));

            perform("/api/analyze?owner=owner&repo=repo")
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").exists());
        }

        @Test
        @DisplayName("error response path field matches the request URI")
        void errorResponse_pathMatchesRequestUri() throws Exception {
            when(analyzerService.analyze(any()))
                    .thenThrow(new RepoNotFoundException("facebook", "gone"));

            perform("/api/analyze?owner=facebook&repo=gone")
                    .andExpect(jsonPath("$.path").value("/api/analyze"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/analyze — Response body structure
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/analyze — response body structure")
    class AnalyzeResponseBody {

        @Test
        @DisplayName("response Content-Type is application/json")
        void responseContentType_isJson() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("response contains top-level fields: owner, repo, stats, healthScore, alerts, languages, contributorActivity, source")
        void responseBody_containsAllTopLevelFields() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.owner").exists())
                    .andExpect(jsonPath("$.repo").exists())
                    .andExpect(jsonPath("$.stats").exists())
                    .andExpect(jsonPath("$.healthScore").exists())
                    .andExpect(jsonPath("$.alerts").exists())
                    .andExpect(jsonPath("$.source").exists());
        }

        @Test
        @DisplayName("response source field is one of LIVE, CACHE, STALE, MOCK")
        void responseBody_sourceIsValidEnum() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$.source")
                            .value(anyOf(is("LIVE"), is("CACHE"), is("STALE"), is("MOCK"))));
        }

        @Test
        @DisplayName("response does NOT contain internal stack trace fields")
        void responseBody_doesNotContainStackTrace() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            String body = perform("/api/analyze?owner=facebook&repo=react")
                    .andReturn().getResponse().getContentAsString();

            // Stack traces must never appear in the response JSON
            org.assertj.core.api.Assertions.assertThat(body)
                    .doesNotContain("at com.githubanalyzer")
                    .doesNotContain("stackTrace")
                    .doesNotContain("StackTrace");
        }

        @Test
        @DisplayName("_links.self.href points to the correct analyze endpoint")
        void hateoasSelfLink_pointsToAnalyzeEndpoint() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$._links.self.href")
                            .value(containsString("/api/analyze")));
        }

        @Test
        @DisplayName("_links.github.href points to the github.com URL")
        void hateoasGithubLink_pointsToGithub() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(jsonPath("$._links.github.href")
                            .value(containsString("github.com")));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/compare — Happy path
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/compare — happy path")
    class CompareHappyPath {

        @Test
        @DisplayName("returns 200 for valid ownerA, repoA, ownerB, repoB")
        void returns200_forValidParams() throws Exception {
            when(analyzerService.compare(any(), any())).thenReturn(buildComparisonResponse());

            performCompare("facebook", "react", "vuejs", "vue")
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("response body contains repoA and repoB")
        void responseBody_containsRepoBoth() throws Exception {
            when(analyzerService.compare(any(), any())).thenReturn(buildComparisonResponse());

            performCompare("facebook", "react", "vuejs", "vue")
                    .andExpect(jsonPath("$.repoA").exists())
                    .andExpect(jsonPath("$.repoB").exists());
        }

        @Test
        @DisplayName("response body contains winner field")
        void responseBody_containsWinner() throws Exception {
            when(analyzerService.compare(any(), any())).thenReturn(buildComparisonResponse());

            performCompare("facebook", "react", "vuejs", "vue")
                    .andExpect(jsonPath("$.winner").exists());
        }

        @Test
        @DisplayName("response body contains healthScoreDelta")
        void responseBody_containsHealthScoreDelta() throws Exception {
            when(analyzerService.compare(any(), any())).thenReturn(buildComparisonResponse());

            performCompare("facebook", "react", "vuejs", "vue")
                    .andExpect(jsonPath("$.healthScoreDelta").isNumber());
        }

        @Test
        @DisplayName("response body contains summary string")
        void responseBody_containsSummary() throws Exception {
            when(analyzerService.compare(any(), any())).thenReturn(buildComparisonResponse());

            performCompare("facebook", "react", "vuejs", "vue")
                    .andExpect(jsonPath("$.summary").isString());
        }

        @Test
        @DisplayName("repoA owner and repo match the ownerA and repoA params")
        void repoA_ownerAndRepoMatchParams() throws Exception {
            when(analyzerService.compare(any(), any())).thenReturn(buildComparisonResponse());

            performCompare("facebook", "react", "vuejs", "vue")
                    .andExpect(jsonPath("$.repoA.owner").value("facebook"))
                    .andExpect(jsonPath("$.repoA.repo").value("react"));
        }

        @Test
        @DisplayName("delegates to analyzerService.compare() with correct request objects")
        void delegatesToService_withCorrectRequests() throws Exception {
            when(analyzerService.compare(any(), any())).thenReturn(buildComparisonResponse());

            performCompare("facebook", "react", "vuejs", "vue");

            verify(analyzerService).compare(
                    argThat(req -> "facebook".equals(req.getOwner()) && "react".equals(req.getRepo())),
                    argThat(req -> "vuejs".equals(req.getOwner()) && "vue".equals(req.getRepo()))
            );
        }

        @Test
        @DisplayName("passes offlineMode=true to both requests when header is set")
        void passesOfflineMode_toBothRequests() throws Exception {
            when(analyzerService.compare(any(), any())).thenReturn(buildComparisonResponse());

            mockMvc.perform(get("/api/compare")
                            .param("ownerA", "facebook").param("repoA", "react")
                            .param("ownerB", "vuejs").param("repoB", "vue")
                            .header("X-Offline-Mode", "true"))
                    .andExpect(status().isOk());

            verify(analyzerService).compare(
                    argThat(req -> req.isOfflineMode()),
                    argThat(req -> req.isOfflineMode())
            );
        }

        @Test
        @DisplayName("winner is 'tie' when both repos have equal health scores")
        void winner_isTie_whenScoresAreEqual() throws Exception {
            AnalysisResponse repoA = buildAnalysisResponse("facebook", "react", DataSource.LIVE, null);
            AnalysisResponse repoB = buildAnalysisResponse("vuejs", "vue", DataSource.LIVE, null);
            // Both have same score (88), so winner is "tie"
            when(analyzerService.compare(any(), any()))
                    .thenReturn(new ComparisonResponse(repoA, repoB));

            performCompare("facebook", "react", "vuejs", "vue")
                    .andExpect(jsonPath("$.winner").value("tie"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/compare — Validation & exceptions
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/compare — validation and exceptions")
    class CompareValidation {

        @Test
        @DisplayName("returns 400 when ownerA is missing")
        void returns400_whenOwnerAMissing() throws Exception {
            mockMvc.perform(get("/api/compare")
                            .param("repoA", "react")
                            .param("ownerB", "vuejs")
                            .param("repoB", "vue"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when repoB is missing")
        void returns400_whenRepoBMissing() throws Exception {
            mockMvc.perform(get("/api/compare")
                            .param("ownerA", "facebook")
                            .param("repoA", "react")
                            .param("ownerB", "vuejs"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when ownerA contains invalid characters")
        void returns400_whenOwnerAContainsInvalidChars() throws Exception {
            mockMvc.perform(get("/api/compare")
                            .param("ownerA", "invalid owner")
                            .param("repoA", "react")
                            .param("ownerB", "vuejs")
                            .param("repoB", "vue"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("returns 404 when RepoNotFoundException is thrown for repoA")
        void returns404_whenRepoANotFound() throws Exception {
            when(analyzerService.compare(any(), any()))
                    .thenThrow(new RepoNotFoundException("facebook", "nonexistent"));

            performCompare("facebook", "nonexistent", "vuejs", "vue")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("REPO_NOT_FOUND"));
        }

        @Test
        @DisplayName("returns 503 when GitHubApiException is thrown during comparison")
        void returns503_whenGitHubApiFailsDuringComparison() throws Exception {
            when(analyzerService.compare(any(), any()))
                    .thenThrow(new GitHubApiException("GitHub down", 503, null));

            performCompare("facebook", "react", "vuejs", "vue")
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("GITHUB_API_ERROR"));
        }

        @Test
        @DisplayName("service is NOT called when compare validation fails")
        void serviceNotCalled_whenCompareValidationFails() throws Exception {
            mockMvc.perform(get("/api/compare")
                            .param("ownerA", "facebook")
                            .param("repoA", "react")
                            // ownerB missing
                            .param("repoB", "vue"));

            verify(analyzerService, never()).compare(any(), any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Cross-cutting: HTTP contract
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("HTTP contract — cross-cutting")
    class HttpContract {

        @Test
        @DisplayName("GET /api/analyze accepts GET method (not POST)")
        void analyzeEndpoint_acceptsGetNotPost() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react")
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/analyze returns JSON Accept header response for default request")
        void analyzeEndpoint_respondsWithJson() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            mockMvc.perform(get("/api/analyze")
                            .param("owner", "facebook")
                            .param("repo", "react")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/compare returns JSON response")
        void compareEndpoint_respondsWithJson() throws Exception {
            when(analyzerService.compare(any(), any())).thenReturn(buildComparisonResponse());

            mockMvc.perform(get("/api/compare")
                            .param("ownerA", "facebook").param("repoA", "react")
                            .param("ownerB", "vuejs").param("repoB", "vue")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("unknown endpoint returns 4xx (not 200)")
        void unknownEndpoint_returns4xx() throws Exception {
            mockMvc.perform(get("/api/unknown"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("analyzerService.compare() is never called when analyze() is requested")
        void analyzeRequest_doesNotCallCompare() throws Exception {
            when(analyzerService.analyze(any())).thenReturn(liveResponse);

            perform("/api/analyze?owner=facebook&repo=react");

            verify(analyzerService, never()).compare(any(), any());
        }

        @Test
        @DisplayName("analyzerService.analyze() is never called when compare() is requested")
        void compareRequest_doesNotCallAnalyze() throws Exception {
            when(analyzerService.compare(any(), any())).thenReturn(buildComparisonResponse());

            performCompare("facebook", "react", "vuejs", "vue");

            verify(analyzerService, never()).analyze(any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers: request performers
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Convenience wrapper for GET requests to arbitrary URLs.
     */
    private ResultActions perform(String url) throws Exception {
        return mockMvc.perform(get(url))
                .andDo(print());
    }

    /**
     * Convenience wrapper for GET /api/compare with all four required params.
     */
    private ResultActions performCompare(String ownerA, String repoA,
                                         String ownerB, String repoB) throws Exception {
        return mockMvc.perform(get("/api/compare")
                        .param("ownerA", ownerA).param("repoA", repoA)
                        .param("ownerB", ownerB).param("repoB", repoB))
                .andDo(print());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers: fixture builders
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Builds a realistic {@link AnalysisResponse} for use across tests.
     * The response has the same health score (88/B) for all repos so that
     * comparison tests produce a deterministic "tie" result.
     */
    private AnalysisResponse buildAnalysisResponse(String owner, String repo,
                                                    DataSource source, Instant cachedAt) {
        RepoStats stats = RepoStats.builder()
                .stars(220000).forks(45000).watchers(6700).openIssues(854)
                .defaultBranch("main")
                .description("The library for web and native user interfaces")
                .license("MIT")
                .hasWiki(true).hasTopics(true)
                .createdAt(Instant.parse("2013-05-29T00:00:00Z"))
                .updatedAt(Instant.now())
                .pushedAt(Instant.now())
                .build();

        HealthScore healthScore = HealthScore.of(
                88, "B",
                new ScoreBreakdown(25, 18, 25, 20));

        List<Alert> alerts = List.of(
                Alert.of(AlertType.HIGH_ISSUE_BACKLOG,
                        "High issue backlog - Maintenance concern",
                        Severity.WARNING));

        LanguageDistribution languages = new LanguageDistribution(
                Map.of("JavaScript", 8200000L, "TypeScript", 1200000L),
                Map.of("JavaScript", 82.5, "TypeScript", 12.3),
                "JavaScript");

        ContributorActivity activity = ContributorActivity.builder()
                .totalContributors(1623)
                .totalCommitsLast52Weeks(892)
                .lastCommitDate(Instant.now())
                .topContributors(List.of(
                        new ContributorSummary("gaearon", 2145, 48)))
                .build();

        Map<String, Map<String, String>> links = Map.of(
                "self",   Map.of("href", "/api/analyze?owner=" + owner + "&repo=" + repo),
                "github", Map.of("href", "https://github.com/" + owner + "/" + repo),
                "compare", Map.of("href", "/api/compare?ownerA=" + owner + "&repoA=" + repo)
        );

        return AnalysisResponse.builder()
                .owner(owner).repo(repo)
                .stats(stats)
                .healthScore(healthScore)
                .alerts(alerts)
                .languages(languages)
                .contributorActivity(activity)
                .source(source)
                .cachedAt(cachedAt)
                .links(links)
                .build();
    }

    /**
     * Builds a {@link ComparisonResponse} where repoA (facebook/react, score 88)
     * and repoB (vuejs/vue, score 88) produce a "tie" — deterministic winner.
     */
    private ComparisonResponse buildComparisonResponse() {
        AnalysisResponse repoA = buildAnalysisResponse("facebook", "react",  DataSource.LIVE, null);
        AnalysisResponse repoB = buildAnalysisResponse("vuejs",    "vue",    DataSource.LIVE, null);
        return new ComparisonResponse(repoA, repoB);
    }
}
