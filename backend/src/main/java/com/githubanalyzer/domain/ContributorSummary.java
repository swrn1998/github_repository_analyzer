package com.githubanalyzer.domain;

/**
 * Summary of a single contributor's activity in a repository.
 */
public final class ContributorSummary {

    private final String login;
    private final int totalCommits;
    private final int weeksActive;

    public ContributorSummary(String login, int totalCommits, int weeksActive) {
        this.login = login;
        this.totalCommits = totalCommits;
        this.weeksActive = weeksActive;
    }

    public String getLogin() { return login; }
    public int getTotalCommits() { return totalCommits; }
    public int getWeeksActive() { return weeksActive; }

    @Override
    public String toString() {
        return "ContributorSummary{login='" + login + "', commits=" + totalCommits + "}";
    }
}
