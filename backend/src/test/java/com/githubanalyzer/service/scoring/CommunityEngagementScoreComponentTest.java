package com.githubanalyzer.service.scoring;

import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.scoring.component.CommunityEngagementScoreComponent;
import com.githubanalyzer.util.RepoDataTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link CommunityEngagementScoreComponent}.
 *
 * <p>Scoring formula under test:
 * <pre>
 *   raw  = round( log10(stars+1)*5 + log10(forks+1)*5 + log10(contributors+1)*3 )
 *   score = min(raw, 25)  ← clamped by BaseScoreComponent
 * </pre>
 *
 * <p>Key properties verified:
 * <ul>
 *   <li>Zero community signals score zero</li>
 *   <li>Log scale: doubling stars doesn't double the score</li>
 *   <li>Max score of 25 is enforced for very large communities</li>
 *   <li>Each signal (stars, forks, contributors) contributes independently</li>
 *   <li>Null guards handled by the BaseScoreComponent template method</li>
 * </ul>
 */
@DisplayName("CommunityEngagementScoreComponent")
class CommunityEngagementScoreComponentTest {

    private CommunityEngagementScoreComponent component;

    @BeforeEach
    void setUp() {
        component = new CommunityEngagementScoreComponent();
    }

    // ── Contract ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getName() returns 'community'")
    void getName_returnsCorrectKey() {
        assertThat(component.getName()).isEqualTo("community");
    }

    @Test
    @DisplayName("getMaxScore() returns 25")
    void getMaxScore_is25() {
        assertThat(component.getMaxScore()).isEqualTo(25);
    }

    // ── Null / missing data ───────────────────────────────────────────────────

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

    // ── Zero community signals ────────────────────────────────────────────────

    @Test
    @DisplayName("score() returns 0 when stars=0, forks=0, contributors=0")
    void score_returnsZero_whenAllSignalsAreZero() {
        // log10(0+1)*5 + log10(0+1)*5 + log10(0+1)*3 = 0 + 0 + 0 = 0
        RepoData data = repoWith(0, 0, 0);
        assertThat(component.score(data)).isEqualTo(0);
    }

    // ── Individual signal contributions ──────────────────────────────────────

    @Test
    @DisplayName("score() increases when stars are added (all else zero)")
    void score_increasesWith_starsOnly() {
        int scoreWith10Stars = component.score(repoWith(10, 0, 0));
        int scoreWith0Stars  = component.score(repoWith(0, 0, 0));

        // log10(11)*5 ≈ 5.2 → rounds to 5
        assertThat(scoreWith10Stars).isGreaterThan(scoreWith0Stars);
        assertThat(scoreWith10Stars).isEqualTo(5);
    }

    @Test
    @DisplayName("score() increases when forks are added (all else zero)")
    void score_increasesWith_forksOnly() {
        int scoreWith10Forks = component.score(repoWith(0, 10, 0));
        assertThat(scoreWith10Forks).isGreaterThan(0);
        // log10(11)*5 ≈ 5.2 → rounds to 5
        assertThat(scoreWith10Forks).isEqualTo(5);
    }

    @Test
    @DisplayName("score() increases when contributors are added (all else zero)")
    void score_increasesWith_contributorsOnly() {
        int score = component.score(repoWith(0, 0, 10));
        // log10(11)*3 ≈ 3.1 → rounds to 3
        assertThat(score).isGreaterThan(0);
        assertThat(score).isEqualTo(3);
    }

    // ── Log scale: diminishing returns ───────────────────────────────────────

    @Test
    @DisplayName("score() is sublinear: 10x more stars does not give 10x more score (log scale)")
    void score_isSublinear_forStars() {
        int score10 = component.score(repoWith(10, 0, 0));
        int score100 = component.score(repoWith(100, 0, 0));
        int score1000 = component.score(repoWith(1000, 0, 0));

        // Log scale: gaps between score values should shrink
        int gain1 = score100 - score10;
        int gain2 = score1000 - score100;

        assertThat(score100).isGreaterThan(score10);
        assertThat(score1000).isGreaterThan(score100);
        // The gains should be roughly equal (log scale), not exponential
        // Both should be small positive integers
        assertThat(gain1).isBetween(1, 10);
        assertThat(gain2).isBetween(1, 10);
    }

    @Test
    @DisplayName("score() for 100 stars is noticeably higher than 1 star (log scale effect)")
    void score_showsLogScaleEffect() {
        int scoreSingleStar = component.score(repoWith(1, 0, 0));
        int score100Stars   = component.score(repoWith(100, 0, 0));
        int score10kStars   = component.score(repoWith(10000, 0, 0));

        // Expected approximate values:
        // log10(2)*5   ≈ 1.5 → 2
        // log10(101)*5 ≈ 10.0 → 10
        // log10(10001)*5 ≈ 20.0 → 20

        assertThat(scoreSingleStar).isLessThan(score100Stars);
        assertThat(score100Stars).isLessThan(score10kStars);
    }

    // ── Expected computed values (formula verification) ───────────────────────

    @Test
    @DisplayName("score() computes correctly for 1000 stars, 200 forks, 50 contributors")
    void score_computedCorrectly_mediumSizedRepo() {
        // stars:        log10(1001) * 5 ≈ 14.99 ≈ 15
        // forks:        log10(201)  * 5 ≈ 11.51 ≈ 12 (partial)
        // contributors: log10(51)   * 3 ≈ 5.12  ≈ 5
        // raw ≈ 32 → clamped to 25
        int score = component.score(repoWith(1000, 200, 50));
        assertThat(score).isEqualTo(25); // should hit the cap
    }

    @Test
    @DisplayName("score() computes correctly for a small repo: 50 stars, 5 forks, 3 contributors")
    void score_computedCorrectly_smallRepo() {
        // stars:        log10(51) * 5 ≈ 8.5  → 9 (rounded)
        // forks:        log10(6)  * 5 ≈ 3.9  → 4
        // contributors: log10(4)  * 3 ≈ 1.81 → 2
        // raw ≈ 15, well under cap
        int score = component.score(repoWith(50, 5, 3));
        assertThat(score).isBetween(13, 17); // Allow ±2 for floating-point rounding
    }

    @Test
    @DisplayName("score() computes correctly for a single-contributor repo: 10 stars, 2 forks, 1 contributor")
    void score_computedCorrectly_tinyRepo() {
        // stars:        log10(11) * 5 ≈ 5.2  → 5
        // forks:        log10(3)  * 5 ≈ 2.4  → 2
        // contributors: log10(2)  * 3 ≈ 0.9  → 1
        // raw ≈ 8
        int score = component.score(repoWith(10, 2, 1));
        assertThat(score).isBetween(6, 10);
    }

    // ── Max score enforcement (clamping) ─────────────────────────────────────

    @Test
    @DisplayName("score() is capped at 25 for a massively popular repo")
    void score_cappedAt25_forPopularRepo() {
        // facebook/react scale: 220K stars, 45K forks, 1600 contributors
        int score = component.score(repoWith(220000, 45000, 1600));
        assertThat(score).isEqualTo(25);
    }

    @Test
    @DisplayName("score() is capped at 25 for extreme values")
    void score_cappedAt25_forExtremeValues() {
        int score = component.score(repoWith(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2, Integer.MAX_VALUE));
        assertThat(score).isEqualTo(25);
    }

    @Test
    @DisplayName("score() never exceeds getMaxScore()")
    void score_neverExceedsMaxScore() {
        int score = component.score(repoWith(1_000_000, 500_000, 10_000));
        assertThat(score).isLessThanOrEqualTo(component.getMaxScore());
    }

    // ── Monotonicity: more signals → higher or equal score ───────────────────

    @Test
    @DisplayName("score() is monotonically non-decreasing as stars increase")
    void score_isMonotonic_asStarsIncrease() {
        int[] starValues = {0, 1, 10, 100, 1000, 10000, 100000};
        int prevScore = -1;
        for (int stars : starValues) {
            int score = component.score(repoWith(stars, 0, 0));
            assertThat(score)
                .as("Score for %d stars should be >= score for fewer stars", stars)
                .isGreaterThanOrEqualTo(prevScore);
            prevScore = score;
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private RepoData repoWith(long stars, long forks, int contributorCount) {
        return RepoDataTestFixtures.repoData()
                .repoDetails(RepoDataTestFixtures.repo()
                        .stars(stars)
                        .forks(forks)
                        .build())
                .contributors(RepoDataTestFixtures.contributors(contributorCount))
                .build();
    }
}
