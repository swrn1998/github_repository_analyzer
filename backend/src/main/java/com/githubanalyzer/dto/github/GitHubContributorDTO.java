package com.githubanalyzer.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw DTO for the GitHub REST API contributor stats response:
 * {@code GET /repos/{owner}/{repo}/stats/contributors}
 *
 * <p>GitHub returns an array where each entry represents one contributor
 * with their weekly commit activity for the last 52 weeks.
 *
 * <p>Note: This endpoint can return HTTP 202 (Accepted) if stats are being
 * computed for the first time. The client should retry after a short delay.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubContributorDTO {

    private GitHubAuthorDTO author;
    private int total;          // Total commits across all weeks
    private List<WeeklyStats> weeks;

    public GitHubAuthorDTO getAuthor() { return author; }
    public void setAuthor(GitHubAuthorDTO author) { this.author = author; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public List<WeeklyStats> getWeeks() { return weeks; }
    public void setWeeks(List<WeeklyStats> weeks) { this.weeks = weeks; }

    /**
     * Returns count of weeks in which at least one commit was made.
     */
    public int getActiveWeeks() {
        if (weeks == null) return 0;
        return (int) weeks.stream().filter(w -> w.getCommits() > 0).count();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GitHubAuthorDTO {
        private String login;
        
        @JsonProperty("avatar_url")
        private String avatarUrl;

        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeeklyStats {
        private long w;  // Week start timestamp (Unix)
        private int a;   // Additions
        private int d;   // Deletions
        private int c;   // Commits

        public long getWeekStart() { return w; }
        public void setW(long w) { this.w = w; }
        public int getAdditions() { return a; }
        public void setA(int a) { this.a = a; }
        public int getDeletions() { return d; }
        public void setD(int d) { this.d = d; }
        public int getCommits() { return c; }
        public void setC(int c) { this.c = c; }
    }
}
