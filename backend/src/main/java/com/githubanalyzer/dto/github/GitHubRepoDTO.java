package com.githubanalyzer.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Raw DTO for the GitHub REST API response:
 * {@code GET /repos/{owner}/{repo}}
 *
 * <p>Only the fields we need are mapped. Unknown fields are ignored
 * ({@code @JsonIgnoreProperties(ignoreUnknown = true)}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepoDTO {

    private String name;
    private String description;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("stargazers_count")
    private long stargazersCount;

    @JsonProperty("forks_count")
    private long forksCount;

    @JsonProperty("watchers_count")
    private long watchersCount;

    @JsonProperty("open_issues_count")
    private long openIssuesCount;

    @JsonProperty("default_branch")
    private String defaultBranch;

    @JsonProperty("has_wiki")
    private boolean hasWiki;

    @JsonProperty("has_topics")
    private boolean hasTopics;

    private String homepage;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("pushed_at")
    private Instant pushedAt;

    private GitHubLicenseDTO license;

    // ── Getters / Setters ─────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public long getStargazersCount() { return stargazersCount; }
    public void setStargazersCount(long stargazersCount) { this.stargazersCount = stargazersCount; }

    public long getForksCount() { return forksCount; }
    public void setForksCount(long forksCount) { this.forksCount = forksCount; }

    public long getWatchersCount() { return watchersCount; }
    public void setWatchersCount(long watchersCount) { this.watchersCount = watchersCount; }

    public long getOpenIssuesCount() { return openIssuesCount; }
    public void setOpenIssuesCount(long openIssuesCount) { this.openIssuesCount = openIssuesCount; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public boolean isHasWiki() { return hasWiki; }
    public void setHasWiki(boolean hasWiki) { this.hasWiki = hasWiki; }

    public boolean isHasTopics() { return hasTopics; }
    public void setHasTopics(boolean hasTopics) { this.hasTopics = hasTopics; }

    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getPushedAt() { return pushedAt; }
    public void setPushedAt(Instant pushedAt) { this.pushedAt = pushedAt; }

    public GitHubLicenseDTO getLicense() { return license; }
    public void setLicense(GitHubLicenseDTO license) { this.license = license; }

    /**
     * Convenience method to extract the license name safely.
     */
    public String getLicenseName() {
        return license != null ? license.getSpdxId() : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GitHubLicenseDTO {
        private String name;

        @JsonProperty("spdx_id")
        private String spdxId;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSpdxId() { return spdxId; }
        public void setSpdxId(String spdxId) { this.spdxId = spdxId; }
    }
}
