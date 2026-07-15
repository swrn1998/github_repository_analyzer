package com.githubanalyzer.domain;

import java.time.Instant;

/**
 * Core statistics for a GitHub repository.
 *
 * <p>Populated directly from the GitHub REST API response
 * ({@code GET /repos/{owner}/{repo}}).
 *
 * <p>Use the inner {@link Builder} for construction.
 */
public final class RepoStats {

    private final long stars;
    private final long forks;
    private final long watchers;
    private final long openIssues;
    private final String defaultBranch;
    private final String description;
    private final String license;
    private final boolean hasWiki;
    private final boolean hasTopics;
    private final String homepage;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant pushedAt;

    private RepoStats(Builder builder) {
        this.stars = builder.stars;
        this.forks = builder.forks;
        this.watchers = builder.watchers;
        this.openIssues = builder.openIssues;
        this.defaultBranch = builder.defaultBranch;
        this.description = builder.description;
        this.license = builder.license;
        this.hasWiki = builder.hasWiki;
        this.hasTopics = builder.hasTopics;
        this.homepage = builder.homepage;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.pushedAt = builder.pushedAt;
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    public long getStars() { return stars; }
    public long getForks() { return forks; }
    public long getWatchers() { return watchers; }
    public long getOpenIssues() { return openIssues; }
    public String getDefaultBranch() { return defaultBranch; }
    public String getDescription() { return description; }
    public String getLicense() { return license; }
    public boolean isHasWiki() { return hasWiki; }
    public boolean isHasTopics() { return hasTopics; }
    public String getHomepage() { return homepage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPushedAt() { return pushedAt; }

    public static Builder builder() {
        return new Builder();
    }

    // ── Builder ─────────────────────────────────────────────────────────────

    public static final class Builder {
        private long stars;
        private long forks;
        private long watchers;
        private long openIssues;
        private String defaultBranch = "main";
        private String description;
        private String license;
        private boolean hasWiki;
        private boolean hasTopics;
        private String homepage;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant pushedAt;

        public Builder stars(long stars) { this.stars = stars; return this; }
        public Builder forks(long forks) { this.forks = forks; return this; }
        public Builder watchers(long watchers) { this.watchers = watchers; return this; }
        public Builder openIssues(long openIssues) { this.openIssues = openIssues; return this; }
        public Builder defaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder license(String license) { this.license = license; return this; }
        public Builder hasWiki(boolean hasWiki) { this.hasWiki = hasWiki; return this; }
        public Builder hasTopics(boolean hasTopics) { this.hasTopics = hasTopics; return this; }
        public Builder homepage(String homepage) { this.homepage = homepage; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder pushedAt(Instant pushedAt) { this.pushedAt = pushedAt; return this; }

        public RepoStats build() {
            return new RepoStats(this);
        }
    }
}
