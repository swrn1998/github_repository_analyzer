package com.githubanalyzer.dto.internal;

import com.githubanalyzer.dto.github.GitHubCommitDTO;
import com.githubanalyzer.dto.github.GitHubContentDTO;
import com.githubanalyzer.dto.github.GitHubContributorDTO;
import com.githubanalyzer.dto.github.GitHubRepoDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Internal aggregate DTO that consolidates all raw data fetched from GitHub
 * before passing it to the enrichment layer (scoring, alerts, etc.).
 *
 * <p>This acts as a parameter object passed between service layers,
 * reducing method parameter lists and keeping the service interfaces clean.
 *
 * <p>All fields may be null if the corresponding API call failed —
 * consumers must handle missing data gracefully.
 */
public class RepoData {

    private final String owner;
    private final String repo;
    private final GitHubRepoDTO repoDetails;
    private final List<GitHubContributorDTO> contributors;
    private final Map<String, Long> languages;
    private final List<GitHubCommitDTO> recentCommits;
    private final List<GitHubContentDTO> rootContents;

    private RepoData(Builder builder) {
        this.owner = builder.owner;
        this.repo = builder.repo;
        this.repoDetails = builder.repoDetails;
        this.contributors = builder.contributors != null
                ? Collections.unmodifiableList(builder.contributors)
                : Collections.emptyList();
        this.languages = builder.languages != null
                ? Collections.unmodifiableMap(builder.languages)
                : Collections.emptyMap();
        this.recentCommits = builder.recentCommits != null
                ? Collections.unmodifiableList(builder.recentCommits)
                : Collections.emptyList();
        this.rootContents = builder.rootContents != null
                ? Collections.unmodifiableList(builder.rootContents)
                : Collections.emptyList();
    }

    // ── Convenience helpers ─────────────────────────────────────────────────

    /**
     * Returns the most recent commit date, or null if no commits are available.
     */
    public java.time.Instant getLastCommitDate() {
        return recentCommits.stream()
                .map(GitHubCommitDTO::getCommitDate)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns true if the root contents contain any recognizable test entry.
     */
    public boolean hasTestFiles() {
        return rootContents.stream().anyMatch(GitHubContentDTO::looksLikeTestEntry);
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public GitHubRepoDTO getRepoDetails() { return repoDetails; }
    public List<GitHubContributorDTO> getContributors() { return contributors; }
    public Map<String, Long> getLanguages() { return languages; }
    public List<GitHubCommitDTO> getRecentCommits() { return recentCommits; }
    public List<GitHubContentDTO> getRootContents() { return rootContents; }

    public static Builder builder() { return new Builder(); }

    // ── Builder ─────────────────────────────────────────────────────────────

    public static final class Builder {
        private String owner;
        private String repo;
        private GitHubRepoDTO repoDetails;
        private List<GitHubContributorDTO> contributors;
        private Map<String, Long> languages;
        private List<GitHubCommitDTO> recentCommits;
        private List<GitHubContentDTO> rootContents;

        public Builder owner(String v) { this.owner = v; return this; }
        public Builder repo(String v) { this.repo = v; return this; }
        public Builder repoDetails(GitHubRepoDTO v) { this.repoDetails = v; return this; }
        public Builder contributors(List<GitHubContributorDTO> v) { this.contributors = v; return this; }
        public Builder languages(Map<String, Long> v) { this.languages = v; return this; }
        public Builder recentCommits(List<GitHubCommitDTO> v) { this.recentCommits = v; return this; }
        public Builder rootContents(List<GitHubContentDTO> v) { this.rootContents = v; return this; }

        public RepoData build() { return new RepoData(this); }
    }
}
