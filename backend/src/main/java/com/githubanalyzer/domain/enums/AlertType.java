package com.githubanalyzer.domain.enums;

/**
 * Classifies the type of health alert raised against a repository.
 *
 * <p>Each alert type corresponds to exactly one {@code AlertRule} implementation,
 * following the Open/Closed Principle — new alert types are added by creating
 * a new enum constant and a new rule class.
 */
public enum AlertType {

    /**
     * No commits detected in the last 90 days.
     * Triggered by {@code InactiveRepoRule}.
     */
    INACTIVE_REPO,

    /**
     * Open issue count exceeds 50.
     * Triggered by {@code HighIssueBacklogRule}.
     */
    HIGH_ISSUE_BACKLOG,

    /**
     * No recognizable test files or directories found.
     * Heuristic detection — not a definitive absence of tests.
     * Triggered by {@code NoTestInfrastructureRule}.
     */
    NO_TESTS,

    /**
     * Dependency filenames or keywords suggest known vulnerable packages.
     * Triggered by {@code SecurityVulnerabilityRule}.
     */
    SECURITY_VULNERABILITIES
}
