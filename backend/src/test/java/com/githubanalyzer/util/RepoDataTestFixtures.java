package com.githubanalyzer.util;

import com.githubanalyzer.dto.github.GitHubCommitDTO;
import com.githubanalyzer.dto.github.GitHubContentDTO;
import com.githubanalyzer.dto.github.GitHubContributorDTO;
import com.githubanalyzer.dto.github.GitHubRepoDTO;
import com.githubanalyzer.dto.internal.RepoData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

/**
 * Shared test fixture factory for building {@link RepoData} objects.
 *
 * <p>Centralises all test data construction so individual test classes
 * stay focused on assertions, not plumbing. Follows the Test Data Builder
 * pattern — each helper produces a sensibly-defaulted object and callers
 * override only the field under test.
 *
 * <p>Usage in test classes:
 * <pre>
 *   RepoData data = RepoDataTestFixtures.repoData()
 *       .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(75).build())
 *       .build();
 * </pre>
 */
public final class RepoDataTestFixtures {

    private RepoDataTestFixtures() { /* utility class */ }

    // ── RepoData builders ────────────────────────────────────────────────────

    /**
     * Returns a RepoData.Builder pre-filled with safe defaults.
     * Only override what the test actually needs to vary.
     */
    public static RepoData.Builder repoData() {
        return RepoData.builder()
                .owner("test-owner")
                .repo("test-repo")
                .repoDetails(repo().build())
                .contributors(Collections.emptyList())
                .recentCommits(Collections.emptyList())
                .rootContents(Collections.emptyList());
    }

    // ── GitHubRepoDTO builders ───────────────────────────────────────────────

    /**
     * Returns a GitHubRepoDTOBuilder pre-filled with safe defaults.
     */
    public static RepoDTOBuilder repo() {
        return new RepoDTOBuilder();
    }

    public static final class RepoDTOBuilder {
        private long stargazersCount = 100;
        private long forksCount = 20;
        private long watchersCount = 10;
        private long openIssuesCount = 5;
        private String description = "A test repository";
        private boolean hasTopics = false;
        private boolean hasWiki = false;
        private String licenseId = null;

        public RepoDTOBuilder stars(long v)            { this.stargazersCount = v; return this; }
        public RepoDTOBuilder forks(long v)            { this.forksCount = v; return this; }
        public RepoDTOBuilder watchers(long v)         { this.watchersCount = v; return this; }
        public RepoDTOBuilder openIssuesCount(long v)  { this.openIssuesCount = v; return this; }
        public RepoDTOBuilder description(String v)    { this.description = v; return this; }
        public RepoDTOBuilder hasTopics(boolean v)     { this.hasTopics = v; return this; }
        public RepoDTOBuilder hasWiki(boolean v)       { this.hasWiki = v; return this; }
        public RepoDTOBuilder licenseId(String v)      { this.licenseId = v; return this; }
        public RepoDTOBuilder noDescription()          { this.description = null; return this; }
        public RepoDTOBuilder noLicense()              { this.licenseId = null; return this; }

        public GitHubRepoDTO build() {
            GitHubRepoDTO dto = new GitHubRepoDTO();
            dto.setStargazersCount(stargazersCount);
            dto.setForksCount(forksCount);
            dto.setWatchersCount(watchersCount);
            dto.setOpenIssuesCount(openIssuesCount);
            dto.setDescription(description);
            dto.setHasTopics(hasTopics);
            dto.setHasWiki(hasWiki);
            dto.setDefaultBranch("main");
            dto.setName("test-repo");

            if (licenseId != null) {
                GitHubRepoDTO.GitHubLicenseDTO license = new GitHubRepoDTO.GitHubLicenseDTO();
                license.setSpdxId(licenseId);
                dto.setLicense(license);
            }
            return dto;
        }
    }

    // ── Commit helpers ───────────────────────────────────────────────────────

    /**
     * Creates a single commit whose date is exactly {@code daysAgo} days before now.
     */
    public static GitHubCommitDTO commitDaysAgo(long daysAgo) {
        Instant commitDate = Instant.now().minus(daysAgo, ChronoUnit.DAYS);
        return commitWithDate(commitDate);
    }

    public static GitHubCommitDTO commitWithDate(Instant date) {
        GitHubCommitDTO dto = new GitHubCommitDTO();
        GitHubCommitDTO.CommitDetail detail = new GitHubCommitDTO.CommitDetail();
        GitHubCommitDTO.CommitAuthor author = new GitHubCommitDTO.CommitAuthor();
        author.setDate(date);
        detail.setAuthor(author);
        dto.setCommit(detail);
        dto.setSha("abc123");
        return dto;
    }

    /** A commit whose nested detail/author objects are null — exercises null-safety. */
    public static GitHubCommitDTO commitWithNullDate() {
        GitHubCommitDTO dto = new GitHubCommitDTO();
        dto.setSha("null-date-sha");
        // commit field is intentionally null
        return dto;
    }

    // ── Content / file helpers ───────────────────────────────────────────────

    public static GitHubContentDTO contentFile(String name) {
        GitHubContentDTO dto = new GitHubContentDTO();
        dto.setName(name);
        dto.setType("file");
        dto.setPath(name);
        return dto;
    }

    public static GitHubContentDTO contentDir(String name) {
        GitHubContentDTO dto = new GitHubContentDTO();
        dto.setName(name);
        dto.setType("dir");
        dto.setPath(name);
        return dto;
    }

    // ── Contributor helpers ──────────────────────────────────────────────────

    public static List<GitHubContributorDTO> contributors(int count) {
        return Collections.nCopies(count, new GitHubContributorDTO());
    }
}
