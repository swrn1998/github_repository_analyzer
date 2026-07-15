package com.githubanalyzer.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Raw DTO for the GitHub REST API commit response:
 * {@code GET /repos/{owner}/{repo}/commits?per_page=1}
 *
 * <p>We fetch only the most recent commit to determine last activity date.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubCommitDTO {

    private String sha;
    private CommitDetail commit;

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }

    public CommitDetail getCommit() { return commit; }
    public void setCommit(CommitDetail commit) { this.commit = commit; }

    /**
     * Safely extracts the author date from the nested commit structure.
     */
    public Instant getCommitDate() {
        if (commit == null || commit.getAuthor() == null) return null;
        return commit.getAuthor().getDate();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitDetail {
        private String message;
        private CommitAuthor author;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public CommitAuthor getAuthor() { return author; }
        public void setAuthor(CommitAuthor author) { this.author = author; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitAuthor {
        private String name;
        private String email;
        private Instant date;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Instant getDate() { return date; }
        public void setDate(Instant date) { this.date = date; }
    }
}
