package com.githubanalyzer.domain;

import java.time.Instant;

/**
 * Represents a point-in-time snapshot of repository metrics.
 * Used for historical trend analysis.
 *
 * <p>Each analysis creates a snapshot that is stored in-memory
 * for trend visualization. Old snapshots are automatically evicted
 * to prevent unbounded memory growth.
 */
public final class TrendSnapshot {

    private final Instant timestamp;
    private final int healthScore;
    private final String grade;
    private final long stars;
    private final long forks;
    private final long openIssues;
    private final int totalContributors;
    private final int commits52Weeks;
    private final String primaryLanguage;

    private TrendSnapshot(Builder builder) {
        this.timestamp = builder.timestamp;
        this.healthScore = builder.healthScore;
        this.grade = builder.grade;
        this.stars = builder.stars;
        this.forks = builder.forks;
        this.openIssues = builder.openIssues;
        this.totalContributors = builder.totalContributors;
        this.commits52Weeks = builder.commits52Weeks;
        this.primaryLanguage = builder.primaryLanguage;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getHealthScore() { return healthScore; }
    public String getGrade() { return grade; }
    public long getStars() { return stars; }
    public long getForks() { return forks; }
    public long getOpenIssues() { return openIssues; }
    public int getTotalContributors() { return totalContributors; }
    public int getCommits52Weeks() { return commits52Weeks; }
    public String getPrimaryLanguage() { return primaryLanguage; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Instant timestamp;
        private int healthScore;
        private String grade;
        private long stars;
        private long forks;
        private long openIssues;
        private int totalContributors;
        private int commits52Weeks;
        private String primaryLanguage;

        public Builder timestamp(Instant v) { this.timestamp = v; return this; }
        public Builder healthScore(int v) { this.healthScore = v; return this; }
        public Builder grade(String v) { this.grade = v; return this; }
        public Builder stars(long v) { this.stars = v; return this; }
        public Builder forks(long v) { this.forks = v; return this; }
        public Builder openIssues(long v) { this.openIssues = v; return this; }
        public Builder totalContributors(int v) { this.totalContributors = v; return this; }
        public Builder commits52Weeks(int v) { this.commits52Weeks = v; return this; }
        public Builder primaryLanguage(String v) { this.primaryLanguage = v; return this; }

        public TrendSnapshot build() { return new TrendSnapshot(this); }
    }
}
