package com.githubanalyzer.service.scoring;

import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.scoring.component.IssueRatioScoreComponent;
import com.githubanalyzer.util.RepoDataTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IssueRatioScoreComponent}.
 *
 * <p>Scoring tiers under test:
 * <pre>
 *   issues = 0        → 25
 *   issues 1–10       → 22
 *   issues 11–30      → 18
 *   issues 31–50      → 13  ← also the HIGH_ISSUE_BACKLOG alert boundary
 *   issues 51–100     → 8
 *   issues 101–200    → 4
 *   issues > 200      → 0
 * </pre>
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Exact boundary values (0, 10, 30, 50, 100, 200, 201)</li>
 *   <li>Values just inside and just outside each boundary</li>
 *   <li>Null RepoData / null repoDetails (BaseScoreComponent guard)</li>
 *   <li>The critical alert-aligned threshold at 50 issues</li>
 * </ul>
 */
@DisplayName("IssueRatioScoreComponent")
class IssueRatioScoreComponentTest {

    private IssueRatioScoreComponent component;

    @BeforeEach
    void setUp() {
        component = new IssueRatioScoreComponent();
    }

    // ── Contract ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getName() returns 'issueRatio'")
    void getName_returnsCorrectKey() {
        assertThat(component.getName()).isEqualTo("issueRatio");
    }

    @Test
    @DisplayName("getMaxScore() returns 25")
    void getMaxScore_is25() {
        assertThat(component.getMaxScore()).isEqualTo(25);
    }

    // ── Null guards (BaseScoreComponent template method) ─────────────────────

    @Test
    @DisplayName("score() returns 0 when RepoData is null")
    void score_returnsZero_whenRepoDataIsNull() {
        assertThat(component.score(null)).isEqualTo(0);
    }

    @Test
    @DisplayName("score() returns 0 when repoDetails is null")
    void score_returnsZero_whenRepoDetailsIsNull() {
        RepoData data = RepoData.builder().owner("o").repo("r").repoDetails(null).build();
        assertThat(component.score(data)).isEqualTo(0);
    }

    // ── Tier: 0 issues → 25 ─────────────────────────────────────────────────

    @Test
    @DisplayName("score() returns 25 for zero open issues (perfect maintenance)")
    void score_returns25_forZeroIssues() {
        assertThat(component.score(repoWithIssues(0))).isEqualTo(25);
    }

    // ── Tier: 1–10 issues → 22 ───────────────────────────────────────────────

    @Test
    @DisplayName("score() returns 22 for 1 open issue (lower boundary of tier)")
    void score_returns22_forOneIssue() {
        assertThat(component.score(repoWithIssues(1))).isEqualTo(22);
    }

    @Test
    @DisplayName("score() returns 22 for exactly 10 open issues (upper boundary)")
    void score_returns22_forExactly10Issues() {
        assertThat(component.score(repoWithIssues(10))).isEqualTo(22);
    }

    @ParameterizedTest(name = "{0} issues → score 22")
    @ValueSource(longs = {1, 5, 9, 10})
    @DisplayName("score() returns 22 for 1–10 open issues")
    void score_returns22_for1To10Issues(long issues) {
        assertThat(component.score(repoWithIssues(issues))).isEqualTo(22);
    }

    // ── Tier: 11–30 issues → 18 ──────────────────────────────────────────────

    @Test
    @DisplayName("score() returns 18 for 11 open issues (boundary: just past 10)")
    void score_returns18_for11Issues() {
        assertThat(component.score(repoWithIssues(11))).isEqualTo(18);
    }

    @Test
    @DisplayName("score() returns 18 for exactly 30 issues (upper boundary)")
    void score_returns18_forExactly30Issues() {
        assertThat(component.score(repoWithIssues(30))).isEqualTo(18);
    }

    @ParameterizedTest(name = "{0} issues → score 18")
    @ValueSource(longs = {11, 20, 29, 30})
    @DisplayName("score() returns 18 for 11–30 open issues")
    void score_returns18_for11To30Issues(long issues) {
        assertThat(component.score(repoWithIssues(issues))).isEqualTo(18);
    }

    // ── Tier: 31–50 issues → 13 (alert-aligned boundary) ────────────────────

    @Test
    @DisplayName("score() returns 13 for 31 issues (just past 30-issue boundary)")
    void score_returns13_for31Issues() {
        assertThat(component.score(repoWithIssues(31))).isEqualTo(13);
    }

    @Test
    @DisplayName("score() returns 13 for exactly 50 issues (HIGH_ISSUE_BACKLOG alert threshold)")
    void score_returns13_forExactly50Issues_alertBoundary() {
        // 50 issues is the LAST value that scores 13.
        // The alert (HIGH_ISSUE_BACKLOG) fires at > 50, so 50 itself does NOT trigger the alert
        // but is still in the degraded score tier.
        assertThat(component.score(repoWithIssues(50))).isEqualTo(13);
    }

    @ParameterizedTest(name = "{0} issues → score 13")
    @ValueSource(longs = {31, 40, 49, 50})
    @DisplayName("score() returns 13 for 31–50 open issues")
    void score_returns13_for31To50Issues(long issues) {
        assertThat(component.score(repoWithIssues(issues))).isEqualTo(13);
    }

    // ── Tier: 51–100 issues → 8 ──────────────────────────────────────────────

    @Test
    @DisplayName("score() returns 8 for 51 issues (first value past alert threshold)")
    void score_returns8_for51Issues() {
        // 51 issues: score drops to 8 AND the HIGH_ISSUE_BACKLOG alert fires
        assertThat(component.score(repoWithIssues(51))).isEqualTo(8);
    }

    @Test
    @DisplayName("score() returns 8 for exactly 100 issues (upper boundary)")
    void score_returns8_forExactly100Issues() {
        assertThat(component.score(repoWithIssues(100))).isEqualTo(8);
    }

    @ParameterizedTest(name = "{0} issues → score 8")
    @ValueSource(longs = {51, 75, 99, 100})
    @DisplayName("score() returns 8 for 51–100 open issues")
    void score_returns8_for51To100Issues(long issues) {
        assertThat(component.score(repoWithIssues(issues))).isEqualTo(8);
    }

    // ── Tier: 101–200 issues → 4 ─────────────────────────────────────────────

    @Test
    @DisplayName("score() returns 4 for 101 issues (just past 100-issue boundary)")
    void score_returns4_for101Issues() {
        assertThat(component.score(repoWithIssues(101))).isEqualTo(4);
    }

    @Test
    @DisplayName("score() returns 4 for exactly 200 issues (upper boundary)")
    void score_returns4_forExactly200Issues() {
        assertThat(component.score(repoWithIssues(200))).isEqualTo(4);
    }

    @ParameterizedTest(name = "{0} issues → score 4")
    @ValueSource(longs = {101, 150, 199, 200})
    @DisplayName("score() returns 4 for 101–200 open issues")
    void score_returns4_for101To200Issues(long issues) {
        assertThat(component.score(repoWithIssues(issues))).isEqualTo(4);
    }

    // ── Tier: > 200 issues → 0 ───────────────────────────────────────────────

    @Test
    @DisplayName("score() returns 0 for 201 issues (first value past 200-issue floor)")
    void score_returnsZero_for201Issues() {
        assertThat(component.score(repoWithIssues(201))).isEqualTo(0);
    }

    @ParameterizedTest(name = "{0} issues → score 0")
    @ValueSource(longs = {201, 500, 1000, 10000})
    @DisplayName("score() returns 0 for extremely high issue counts")
    void score_returnsZero_forHighIssueCounts(long issues) {
        assertThat(component.score(repoWithIssues(issues))).isEqualTo(0);
    }

    // ── Boundary table: exhaustive cross-boundary check ──────────────────────

    @ParameterizedTest(name = "{0} issues → {1} points")
    @CsvSource({
        "0,   25",
        "1,   22",
        "10,  22",
        "11,  18",
        "30,  18",
        "31,  13",
        "50,  13",
        "51,   8",
        "100,  8",
        "101,  4",
        "200,  4",
        "201,  0",
        "9999, 0"
    })
    @DisplayName("score() boundary table: all tier transitions")
    void score_boundaryTable(long issues, int expectedScore) {
        assertThat(component.score(repoWithIssues(issues))).isEqualTo(expectedScore);
    }

    // ── Clamping guarantees ───────────────────────────────────────────────────

    @Test
    @DisplayName("score() never exceeds 25")
    void score_neverExceedsMaxScore() {
        assertThat(component.score(repoWithIssues(0))).isLessThanOrEqualTo(25);
    }

    @Test
    @DisplayName("score() is always non-negative")
    void score_isAlwaysNonNegative() {
        assertThat(component.score(repoWithIssues(Long.MAX_VALUE))).isGreaterThanOrEqualTo(0);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private RepoData repoWithIssues(long issueCount) {
        return RepoDataTestFixtures.repoData()
                .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(issueCount).build())
                .build();
    }
}
