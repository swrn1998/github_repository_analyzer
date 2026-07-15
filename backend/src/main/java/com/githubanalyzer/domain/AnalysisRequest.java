package com.githubanalyzer.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Incoming request to analyze a GitHub repository.
 *
 * <p>Validation is applied via Bean Validation annotations.
 * The {@code owner} and {@code repo} must match GitHub's naming rules:
 * alphanumeric, hyphens, underscores, and dots only.
 */
public class AnalysisRequest {

    private static final String GITHUB_NAME_PATTERN = "^[a-zA-Z0-9._-]{1,100}$";
    private static final String INVALID_NAME_MSG =
            "Must be 1–100 characters: letters, digits, hyphens, underscores, or dots only";

    @NotBlank(message = "owner must not be blank")
    @Pattern(regexp = GITHUB_NAME_PATTERN, message = INVALID_NAME_MSG)
    private String owner;

    @NotBlank(message = "repo must not be blank")
    @Pattern(regexp = GITHUB_NAME_PATTERN, message = INVALID_NAME_MSG)
    private String repo;

    private boolean offlineMode;

    // ── Constructors ─────────────────────────────────────────────────────────

    public AnalysisRequest() {}

    public AnalysisRequest(String owner, String repo, boolean offlineMode) {
        this.owner = owner;
        this.repo = repo;
        this.offlineMode = offlineMode;
    }

    // ── Derived ──────────────────────────────────────────────────────────────

    /**
     * Returns the canonical "owner/repo" identifier used as a cache key.
     * Always lowercase to ensure consistent cache lookups.
     */
    public String getCacheKey() {
        return (owner + ":" + repo).toLowerCase();
    }

    /**
     * Returns the "owner/repo" format for logging and display.
     */
    public String getFullName() {
        return owner + "/" + repo;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getRepo() { return repo; }
    public void setRepo(String repo) { this.repo = repo; }

    public boolean isOfflineMode() { return offlineMode; }
    public void setOfflineMode(boolean offlineMode) { this.offlineMode = offlineMode; }

    @Override
    public String toString() {
        return "AnalysisRequest{owner='" + owner + "', repo='" + repo
                + "', offline=" + offlineMode + "}";
    }
}
