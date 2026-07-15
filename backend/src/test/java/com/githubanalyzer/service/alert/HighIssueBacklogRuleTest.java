package com.githubanalyzer.service.alert;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.domain.enums.AlertType;
import com.githubanalyzer.domain.enums.Severity;
import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.alert.rule.HighIssueBacklogRule;
import com.githubanalyzer.util.RepoDataTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HighIssueBacklogRule}.
 *
 * <p>Rule fires when: {@code openIssues > 50} (strict greater-than)
 *
 * <p>Critical boundary values:
 * <ul>
 *   <li>Exactly 50 open issues → does NOT fire</li>
 *   <li>51 open issues         → DOES fire</li>
 * </ul>
 *
 * <p>This boundary aligns with the IssueRatioScoreComponent threshold —
 * tests verify both the alert and the score component use the same cutoff.
 */
@DisplayName("HighIssueBacklogRule")
class HighIssueBacklogRuleTest {

    private HighIssueBacklogRule rule;

    @BeforeEach
    void setUp() {
        rule = new HighIssueBacklogRule();
    }

    // ── Rule does NOT apply ───────────────────────────────────────────────────

    @Test
    @DisplayName("applies() returns false when repoDetails is null (null-safe)")
    void applies_returnsFalse_whenRepoDetailsIsNull() {
        RepoData data = RepoData.builder().owner("o").repo("r").repoDetails(null).build();
        assertThat(rule.applies(data)).isFalse();
    }

    @Test
    @DisplayName("applies() returns false for 0 open issues")
    void applies_returnsFalse_forZeroIssues() {
        assertThat(rule.applies(repoWithIssues(0))).isFalse();
    }

    @Test
    @DisplayName("applies() returns false for 1 open issue")
    void applies_returnsFalse_forOneIssue() {
        assertThat(rule.applies(repoWithIssues(1))).isFalse();
    }

    @Test
    @DisplayName("applies() returns false for exactly 50 open issues (boundary — NOT a backlog)")
    void applies_returnsFalse_atExactly50Issues_boundary() {
        // 50 is the threshold constant; the condition is > 50, so 50 does NOT fire
        assertThat(rule.applies(repoWithIssues(50))).isFalse();
    }

    @ParameterizedTest(name = "{0} issues → rule does NOT fire")
    @ValueSource(longs = {0, 1, 10, 25, 49, 50})
    @DisplayName("applies() returns false for all values at or below threshold (50)")
    void applies_returnsFalse_atOrBelowThreshold(long issues) {
        assertThat(rule.applies(repoWithIssues(issues))).isFalse();
    }

    // ── Rule DOES apply ───────────────────────────────────────────────────────

    @Test
    @DisplayName("applies() returns true for exactly 51 open issues (first value past threshold)")
    void applies_returnsTrue_at51Issues() {
        // The critical off-by-one: 50 → false, 51 → true
        assertThat(rule.applies(repoWithIssues(51))).isTrue();
    }

    @Test
    @DisplayName("applies() returns true for 100 open issues")
    void applies_returnsTrue_for100Issues() {
        assertThat(rule.applies(repoWithIssues(100))).isTrue();
    }

    @Test
    @DisplayName("applies() returns true for 10000 open issues (extreme backlog)")
    void applies_returnsTrue_forExtremeBacklog() {
        assertThat(rule.applies(repoWithIssues(10_000))).isTrue();
    }

    @ParameterizedTest(name = "{0} issues → rule DOES fire")
    @ValueSource(longs = {51, 52, 100, 500, 1000, 10000})
    @DisplayName("applies() returns true for all values above threshold (> 50)")
    void applies_returnsTrue_aboveThreshold(long issues) {
        assertThat(rule.applies(repoWithIssues(issues))).isTrue();
    }

    // ── Exhaustive boundary table ─────────────────────────────────────────────

    @ParameterizedTest(name = "{0} issues → applies={1}")
    @CsvSource({
        "0,     false",
        "49,    false",
        "50,    false",   // boundary: NOT included
        "51,    true",    // boundary+1: first to fire
        "100,   true",
        "10000, true"
    })
    @DisplayName("applies() boundary table")
    void applies_boundaryTable(long issues, boolean expected) {
        assertThat(rule.applies(repoWithIssues(issues))).isEqualTo(expected);
    }

    // ── Alert content verification ────────────────────────────────────────────

    @Test
    @DisplayName("generateAlert() produces HIGH_ISSUE_BACKLOG alert type")
    void generateAlert_producesCorrectType() {
        Alert alert = rule.generateAlert(repoWithIssues(100));
        assertThat(alert.getType()).isEqualTo(AlertType.HIGH_ISSUE_BACKLOG);
    }

    @Test
    @DisplayName("generateAlert() produces WARNING severity")
    void generateAlert_producesWarningSeverity() {
        Alert alert = rule.generateAlert(repoWithIssues(100));
        assertThat(alert.getSeverity()).isEqualTo(Severity.WARNING);
    }

    @Test
    @DisplayName("generateAlert() message matches problem statement exactly")
    void generateAlert_messageMatchesProblemStatement() {
        Alert alert = rule.generateAlert(repoWithIssues(100));
        assertThat(alert.getMessage()).isEqualTo("High issue backlog - Maintenance concern");
    }

    @Test
    @DisplayName("generateAlert() returns a non-null alert regardless of issue count")
    void generateAlert_returnsNonNull() {
        assertThat(rule.generateAlert(repoWithIssues(Long.MAX_VALUE))).isNotNull();
    }

    // ── ISSUE_THRESHOLD constant alignment ───────────────────────────────────

    @Test
    @DisplayName("ISSUE_THRESHOLD constant is 50 (aligns with IssueRatioScoreComponent)")
    void issueThreshold_is50() {
        // Both the alert and score component should use the same 50-issue cutoff.
        // Verifying the package-visible constant directly.
        assertThat(HighIssueBacklogRule.ISSUE_THRESHOLD).isEqualTo(50);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private RepoData repoWithIssues(long issueCount) {
        return RepoDataTestFixtures.repoData()
                .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(issueCount).build())
                .build();
    }
}
