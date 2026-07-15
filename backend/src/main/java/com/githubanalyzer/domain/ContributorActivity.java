package com.githubanalyzer.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated contributor activity data for a repository.
 *
 * <p>Sourced from {@code GET /repos/{owner}/{repo}/stats/contributors}.
 */
public final class ContributorActivity {

    private final int totalContributors;
    private final List<ContributorSummary> topContributors;
    private final int totalCommitsLast52Weeks;
    private final Instant lastCommitDate;

    private ContributorActivity(Builder builder) {
        this.totalContributors = builder.totalContributors;
        this.topContributors = Collections.unmodifiableList(builder.topContributors);
        this.totalCommitsLast52Weeks = builder.totalCommitsLast52Weeks;
        this.lastCommitDate = builder.lastCommitDate;
    }

    public int getTotalContributors() { return totalContributors; }
    public List<ContributorSummary> getTopContributors() { return topContributors; }
    public int getTotalCommitsLast52Weeks() { return totalCommitsLast52Weeks; }
    public Instant getLastCommitDate() { return lastCommitDate; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int totalContributors;
        private List<ContributorSummary> topContributors = Collections.emptyList();
        private int totalCommitsLast52Weeks;
        private Instant lastCommitDate;

        public Builder totalContributors(int v) { this.totalContributors = v; return this; }
        public Builder topContributors(List<ContributorSummary> v) { this.topContributors = v; return this; }
        public Builder totalCommitsLast52Weeks(int v) { this.totalCommitsLast52Weeks = v; return this; }
        public Builder lastCommitDate(Instant v) { this.lastCommitDate = v; return this; }

        public ContributorActivity build() { return new ContributorActivity(this); }
    }
}
