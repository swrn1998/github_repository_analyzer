package com.githubanalyzer.controller.advice;

import com.githubanalyzer.exception.GitHubApiException;
import com.githubanalyzer.exception.RateLimitExceededException;
import com.githubanalyzer.exception.RepoNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handler for all REST controllers.
 *
 * <p>Maps domain exceptions to appropriate HTTP status codes and
 * structured error response bodies. Ensures no internal exception
 * details leak to API consumers in production.
 *
 * <p>All handlers log the error at an appropriate level:
 * <ul>
 *   <li>4xx errors: WARN (expected client errors)</li>
 *   <li>5xx errors: ERROR (unexpected server errors)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Domain exceptions ────────────────────────────────────────────────────

    @ExceptionHandler(RepoNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRepoNotFound(
            RepoNotFoundException ex, HttpServletRequest request) {
        log.warn("Repository not found: {}/{}", ex.getOwner(), ex.getRepo());
        return buildError(HttpStatus.NOT_FOUND, "REPO_NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(
            RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("GitHub rate limit exceeded. Resets at: {}", ex.getResetEpochSeconds());
        return buildError(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                "GitHub API rate limit exceeded. Please try again later.", request.getRequestURI());
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<Map<String, Object>> handleGitHubApiError(
            GitHubApiException ex, HttpServletRequest request) {
        log.error("GitHub API error (status={}): {}", ex.getStatusCode(), ex.getMessage());
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, "GITHUB_API_ERROR",
                "GitHub is currently unavailable. Please try again later.", request.getRequestURI());
    }

    // ── Validation exceptions ────────────────────────────────────────────────

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst()
                .orElse("Validation failed");
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message, request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing required parameter: {}", ex.getParameterName());
        return buildError(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing", request.getRequestURI());
    }

    // ── Catch-all ────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again.", request.getRequestURI());
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String code, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
}
