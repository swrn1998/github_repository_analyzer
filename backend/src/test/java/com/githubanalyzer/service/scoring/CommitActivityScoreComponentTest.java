package com.githubanalyzer.service.scoring;

import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.scoring.component.CommitActivityScoreComponent;
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
 * Unit tests for {@link CommitActivityScoreComponent}.
 *
 * <p>Strategy under test:
 * <ul>
 *   <li>≤  7 days  → 25</li>
 *   <li>≤ 30 days  → 20</li>
 *   <li>≤ 60 days  → 13</li>
 *   <li>≤ 90 days  → 5</li>
 *   <li>> 90 days  → 0</li>
 *   <li>null date  → 10 (data unavailable)</li>
 *   <li>null repoData / null repoDetails → 0 (BaseScoreComponent null guard)</li>
 * </ul>
 */
@DisplayName("CommitActivityScoreComponent")
class CommitActivityScoreComponentTest {

    private CommitActivityScoreComponent component;

    @BeforeEach
    void setUp() {
        component = new CommitActivityScoreComponent();
    }

    // ── Contract ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getName() returns 'commitActivity'")
    void getName_returnsCorrectKey() {
        assertThat(component.getName()).isEqualTo("commitActivity");
    }

    @Test
    @DisplayName("getMaxScore() returns 25")
    void getMaxScore_is25() {
        assertThat(component.getMaxScore()).isEqualTo(25);
    }

    // ── Null / missing data (BaseScoreComponent template method guard) ────────

    @Test
    @DisplayName("score() returns 0 when RepoData is null")
    void score_returnsZero_whenRepoDataIsNull() {
        assertThat(component.score(null)).isEqualTo(0);
    }

    @Test
    @DisplayName("score() returns 0 when repoDetails is null")
    void score_returnsZero_whenRepoDetailsIsNull() {
        RepoData data = RepoData.builder()
                .owner("o").repo("r")
                .repoDetails(null)   // explicitly null
                .build();

        assertThat(component.score(data)).isEqualTo(0);
    }

    @Test
    @DisplayName("score() returns 10 when commit list is empty (data unavailable)")
    void score_returns10_whenNoCommitsAvailable() {
        RepoData data = RepoDataTestFixtures.repoData()
                .recentCommits(Collections.emptyList())
                .build();

        assertThat(component.score(data)).isEqualTo(10);
    }

    @Test
    @DisplayName("score() returns 10 when commit has null date (null-safe)")
    void score_returns10_whenCommitDateIsNull() {
        RepoData data = RepoDataTestFixtures.repoData()
                .recentCommits(List.of(RepoDataTestFixtures.commitWithNullDate()))
                .build();

        // null date → getLastCommitDate() returns null → treated as unavailable → 10
        assertThat(component.score(data)).isEqualTo(10);
    }

    // ── Boundary values: ≤ 7 days → 25 ──────────────────────────────────────

    @Test
    @DisplayName("score() returns 25 for commit exactly today (0 days ago)")
    void score_returns25_forCommitToday() {
        RepoData data = repoDataWithCommitDaysAgo(0);
        assertThat(component.score(data)).isEqualTo(25);
    }

    @Test
    @DisplayName("score() returns 25 for commit exactly 7 days ago (boundary)")
    void score_returns25_atExactlySevenDaysBoundary() {
        RepoData data = repoDataWithCommitDaysAgo(7);
        assertThat(component.score(data)).isEqualTo(25);
    }

    @ParameterizedTest(name = "{0} days ago → score 25")
    @ValueSource(longs = {1, 3, 5, 6, 7})
    @DisplayName("score() returns 25 for commits within 7-day window")
    void score_returns25_withinSevenDays(long daysAgo) {
        RepoData data = repoDataWithCommitDaysAgo(daysAgo);
        assertThat(component.score(data)).isEqualTo(25);
    }

    // ── Boundary values: 8–30 days → 20 ──────────────────────────────────────

    @Test
    @DisplayName("score() returns 20 for commit 8 days ago (just outside 7-day window)")
    void score_returns20_forCommit8DaysAgo() {
        RepoData data = repoDataWithCommitDaysAgo(8);
        assertThat(component.score(data)).isEqualTo(20);
    }

    @Test
    @DisplayName("score() returns 20 for commit exactly 30 days ago (boundary)")
    void score_returns20_atExactly30DaysBoundary() {
        RepoData data = repoDataWithCommitDaysAgo(30);
        assertThat(component.score(data)).isEqualTo(20);
    }

    @ParameterizedTest(name = "{0} days ago → score 20")
    @ValueSource(longs = {8, 15, 20, 29, 30})
    @DisplayName("score() returns 20 for commits within 8–30 day window")
    void score_returns20_in8To30DayRange(long daysAgo) {
        RepoData data = repoDataWithCommitDaysAgo(daysAgo);
        assertThat(component.score(data)).isEqualTo(20);
    }

    // ── Boundary values: 31–60 days → 13 ────────────────────────────────────

    @Test
    @DisplayName("score() returns 13 for commit 31 days ago (just past 30-day boundary)")
    void score_returns13_forCommit31DaysAgo() {
        RepoData data = repoDataWithCommitDaysAgo(31);
        assertThat(component.score(data)).isEqualTo(13);
    }

    @Test
    @DisplayName("score() returns 13 for commit exactly 60 days ago (boundary)")
    void score_returns13_atExactly60DaysBoundary() {
        RepoData data = repoDataWithCommitDaysAgo(60);
        assertThat(component.score(data)).isEqualTo(13);
    }

    @ParameterizedTest(name = "{0} days ago → score 13")
    @ValueSource(longs = {31, 45, 59, 60})
    @DisplayName("score() returns 13 for commits within 31–60 day window")
    void score_returns13_in31To60DayRange(long daysAgo) {
        RepoData data = repoDataWithCommitDaysAgo(daysAgo);
        assertThat(component.score(data)).isEqualTo(13);
    }

    // ── Boundary values: 61–90 days → 5 ─────────────────────────────────────

    @Test
    @DisplayName("score() returns 5 for commit 61 days ago (just past 60-day boundary)")
    void score_returns5_forCommit61DaysAgo() {
        RepoData data = repoDataWithCommitDaysAgo(61);
        assertThat(component.score(data)).isEqualTo(5);
    }

    @Test
    @DisplayName("score() returns 5 for commit exactly 90 days ago (boundary — NOT inactive)")
    void score_returns5_atExactly90DaysBoundary() {
        // 90 days is the LAST day that scores 5 (condition: daysSince <= 90)
        RepoData data = repoDataWithCommitDaysAgo(90);
        assertThat(component.score(data)).isEqualTo(5);
    }

    @ParameterizedTest(name = "{0} days ago → score 5")
    @ValueSource(longs = {61, 75, 89, 90})
    @DisplayName("score() returns 5 for commits within 61–90 day window")
    void score_returns5_in61To90DayRange(long daysAgo) {
        RepoData data = repoDataWithCommitDaysAgo(daysAgo);
        assertThat(component.score(data)).isEqualTo(5);
    }

    // ── Boundary values: > 90 days → 0 ──────────────────────────────────────

    @Test
    @DisplayName("score() returns 0 for commit 91 days ago (first day past threshold)")
    void score_returnsZero_forCommit91DaysAgo() {
        // Key boundary: 90 days scores 5, but 91 days scores 0
        RepoData data = repoDataWithCommitDaysAgo(91);
        assertThat(component.score(data)).isEqualTo(0);
    }

    @Test
    @DisplayName("score() returns 0 for commit 1 year ago")
    void score_returnsZero_forCommit365DaysAgo() {
        RepoData data = repoDataWithCommitDaysAgo(365);
        assertThat(component.score(data)).isEqualTo(0);
    }

    @ParameterizedTest(name = "{0} days ago → score 0 (inactive)")
    @ValueSource(longs = {91, 100, 180, 365, 1000})
    @DisplayName("score() returns 0 for commits older than 90 days")
    void score_returnsZero_forInactiveRepository(long daysAgo) {
        RepoData data = repoDataWithCommitDaysAgo(daysAgo);
        assertThat(component.score(data)).isEqualTo(0);
    }

    // ── Clamping (BaseScoreComponent guarantees) ──────────────────────────────

    @Test
    @DisplayName("score() never exceeds getMaxScore() (25)")
    void score_neverExceedsMaxScore() {
        RepoData data = repoDataWithCommitDaysAgo(0); // Best case
        assertThat(component.score(data)).isLessThanOrEqualTo(component.getMaxScore());
    }

    @Test
    @DisplayName("score() is always non-negative")
    void score_isAlwaysNonNegative() {
        RepoData data = repoDataWithCommitDaysAgo(9999); // Worst case
        assertThat(component.score(data)).isGreaterThanOrEqualTo(0);
    }

    // ── Multiple commits: only the first (most recent) matters ───────────────

    @Test
    @DisplayName("score() uses the first commit's date when multiple commits exist")
    void score_usesFirstCommitDate_whenMultipleCommitsProvided() {
        // First commit is recent (score 25), second is old — should use first
        RepoData data = RepoDataTestFixtures.repoData()
                .recentCommits(List.of(
                        RepoDataTestFixtures.commitDaysAgo(2),    // recent → 25
                        RepoDataTestFixtures.commitDaysAgo(200)   // old → would be 0
                ))
                .build();

        assertThat(component.score(data)).isEqualTo(25);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private RepoData repoDataWithCommitDaysAgo(long days) {
        return RepoDataTestFixtures.repoData()
                .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(days)))
                .build();
    }
}
