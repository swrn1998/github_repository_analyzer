package com.githubanalyzer.service.alert;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.domain.enums.AlertType;
import com.githubanalyzer.domain.enums.Severity;
import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.alert.rule.InactiveRepoRule;
import com.githubanalyzer.util.RepoDataTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InactiveRepoRule}.
 *
 * <p>Rule fires when: {@code daysSince(lastCommit) > 90}
 *
 * <p>Critical boundary: the rule uses strict greater-than (> 90), so:
 * <ul>
 *   <li>Exactly 90 days old → does NOT fire</li>
 *   <li>91 days old         → DOES fire</li>
 * </ul>
 *
 * <p>The rule does NOT fire when commit date is null
 * (data absent ≠ data confirming inactivity).
 */
@DisplayName("InactiveRepoRule")
class InactiveRepoRuleTest {

    private InactiveRepoRule rule;

    @BeforeEach
    void setUp() {
        rule = new InactiveRepoRule();
    }

    // ── Rule does NOT apply ───────────────────────────────────────────────────

    @Nested
    @DisplayName("applies() returns false (rule should NOT fire)")
    class RuleDoesNotApply {

        @Test
        @DisplayName("applies() returns false when commit date is null (data unavailable)")
        void applies_returnsFalse_whenLastCommitDateIsNull() {
            // A null commit date means the API call failed — we don't alert on missing data
            RepoData data = RepoDataTestFixtures.repoData()
                    .recentCommits(Collections.emptyList())
                    .build();

            assertThat(rule.applies(data)).isFalse();
        }

        @Test
        @DisplayName("applies() returns false when commit has null date nested object")
        void applies_returnsFalse_whenCommitHasNullDateObject() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .recentCommits(List.of(RepoDataTestFixtures.commitWithNullDate()))
                    .build();

            assertThat(rule.applies(data)).isFalse();
        }

        @Test
        @DisplayName("applies() returns false for commit today (0 days ago)")
        void applies_returnsFalse_forCommitToday() {
            RepoData data = repoDataWithCommitDaysAgo(0);
            assertThat(rule.applies(data)).isFalse();
        }

        @Test
        @DisplayName("applies() returns false for commit 1 day ago")
        void applies_returnsFalse_forCommitYesterday() {
            RepoData data = repoDataWithCommitDaysAgo(1);
            assertThat(rule.applies(data)).isFalse();
        }

        @Test
        @DisplayName("applies() returns false for commit exactly 90 days ago (boundary — NOT inactive)")
        void applies_returnsFalse_atExactly90DaysBoundary() {
            // The rule is STRICTLY greater-than: daysSince > 90
            // At exactly 90 days, the repo is at the edge but NOT considered inactive
            RepoData data = repoDataWithCommitDaysAgo(90);
            assertThat(rule.applies(data)).isFalse();
        }

        @ParameterizedTest(name = "{0} days ago → does NOT fire")
        @ValueSource(longs = {0, 1, 7, 30, 60, 89, 90})
        @DisplayName("applies() returns false for all values within the 90-day threshold")
        void applies_returnsFalse_withinThreshold(long daysAgo) {
            RepoData data = repoDataWithCommitDaysAgo(daysAgo);
            assertThat(rule.applies(data)).isFalse();
        }
    }

    // ── Rule DOES apply ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("applies() returns true (rule SHOULD fire)")
    class RuleDoesApply {

        @Test
        @DisplayName("applies() returns true for commit exactly 91 days ago (first day past threshold)")
        void applies_returnsTrue_at91Days() {
            // This is the critical off-by-one: 90 days → false, 91 days → true
            RepoData data = repoDataWithCommitDaysAgo(91);
            assertThat(rule.applies(data)).isTrue();
        }

        @Test
        @DisplayName("applies() returns true for commit 6 months ago")
        void applies_returnsTrue_for6MonthsAgo() {
            RepoData data = repoDataWithCommitDaysAgo(180);
            assertThat(rule.applies(data)).isTrue();
        }

        @Test
        @DisplayName("applies() returns true for commit 1 year ago")
        void applies_returnsTrue_for365DaysAgo() {
            RepoData data = repoDataWithCommitDaysAgo(365);
            assertThat(rule.applies(data)).isTrue();
        }

        @Test
        @DisplayName("applies() returns true for commit 5 years ago (abandoned repo)")
        void applies_returnsTrue_forAbandonedRepo() {
            RepoData data = repoDataWithCommitDaysAgo(365 * 5);
            assertThat(rule.applies(data)).isTrue();
        }

        @ParameterizedTest(name = "{0} days ago → DOES fire")
        @ValueSource(longs = {91, 92, 100, 180, 365, 730, 1825})
        @DisplayName("applies() returns true for all values past the 90-day threshold")
        void applies_returnsTrue_pastThreshold(long daysAgo) {
            RepoData data = repoDataWithCommitDaysAgo(daysAgo);
            assertThat(rule.applies(data)).isTrue();
        }
    }

    // ── Alert content verification ────────────────────────────────────────────

    @Test
    @DisplayName("generateAlert() produces INACTIVE_REPO type alert")
    void generateAlert_producesCorrectAlertType() {
        RepoData data = repoDataWithCommitDaysAgo(100);
        Alert alert = rule.generateAlert(data);

        assertThat(alert.getType()).isEqualTo(AlertType.INACTIVE_REPO);
    }

    @Test
    @DisplayName("generateAlert() produces WARNING severity alert")
    void generateAlert_producesWarningSeverity() {
        RepoData data = repoDataWithCommitDaysAgo(100);
        Alert alert = rule.generateAlert(data);

        assertThat(alert.getSeverity()).isEqualTo(Severity.WARNING);
    }

    @Test
    @DisplayName("generateAlert() message matches problem statement exactly")
    void generateAlert_messageMatchesProblemStatement() {
        RepoData data = repoDataWithCommitDaysAgo(100);
        Alert alert = rule.generateAlert(data);

        assertThat(alert.getMessage()).isEqualTo("Repository appears inactive");
    }

    @Test
    @DisplayName("generateAlert() returns non-null alert")
    void generateAlert_returnsNonNull() {
        RepoData data = repoDataWithCommitDaysAgo(100);
        assertThat(rule.generateAlert(data)).isNotNull();
    }

    // ── applies() + generateAlert() coherence ────────────────────────────────

    @Test
    @DisplayName("applies() + generateAlert() work together: active repo produces no alert")
    void coherence_activeRepoProducesNoAlert() {
        RepoData data = repoDataWithCommitDaysAgo(5);

        boolean shouldAlert = rule.applies(data);
        assertThat(shouldAlert).isFalse();
        // If applies() is false, generateAlert() is never called — behaviour is correct
    }

    @Test
    @DisplayName("applies() + generateAlert() work together: inactive repo produces valid alert")
    void coherence_inactiveRepoProducesValidAlert() {
        RepoData data = repoDataWithCommitDaysAgo(200);

        boolean shouldAlert = rule.applies(data);
        assertThat(shouldAlert).isTrue();

        Alert alert = rule.generateAlert(data);
        assertThat(alert).isNotNull();
        assertThat(alert.getType()).isEqualTo(AlertType.INACTIVE_REPO);
        assertThat(alert.getSeverity()).isEqualTo(Severity.WARNING);
        assertThat(alert.getMessage()).isNotBlank();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private RepoData repoDataWithCommitDaysAgo(long days) {
        return RepoDataTestFixtures.repoData()
                .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(days)))
                .build();
    }
}
