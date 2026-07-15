package com.githubanalyzer.service.scoring;

import com.githubanalyzer.domain.HealthScore;
import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.scoring.component.*;
import com.githubanalyzer.service.scoring.impl.WeightedHealthScoreCalculator;
import com.githubanalyzer.util.RepoDataTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WeightedHealthScoreCalculator}.
 *
 * <p>Two test approaches:
 * <ol>
 *   <li><b>Mocked components</b> — verifies the calculator correctly sums scores,
 *       maps grades, and builds the ScoreBreakdown without depending on component logic.</li>
 *   <li><b>Real components integration</b> — verifies the wired-together system
 *       produces sensible scores for well-known repository archetypes.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WeightedHealthScoreCalculator")
class WeightedHealthScoreCalculatorTest {

    @Mock private CommitActivityScoreComponent commitComponent;
    @Mock private IssueRatioScoreComponent issueComponent;
    @Mock private CommunityEngagementScoreComponent communityComponent;
    @Mock private DocumentationQualityScoreComponent documentationComponent;

    private ScoreGradeMapper gradeMapper;
    private WeightedHealthScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        gradeMapper = new ScoreGradeMapper();
        calculator = new WeightedHealthScoreCalculator(
                commitComponent,
                issueComponent,
                communityComponent,
                documentationComponent,
                gradeMapper);
    }

    // ── Total score computation ───────────────────────────────────────────────

    @Test
    @DisplayName("calculate() sums all four component scores correctly")
    void calculate_sumsAllFourComponentScores() {
        stubComponents(20, 18, 22, 15);  // total = 75

        HealthScore result = calculator.calculate(anyRepoData());

        assertThat(result.getScore()).isEqualTo(75);
    }

    @Test
    @DisplayName("calculate() returns score = 100 when all components return 25 (perfect)")
    void calculate_returns100_whenAllComponentsReturn25() {
        stubComponents(25, 25, 25, 25);

        HealthScore result = calculator.calculate(anyRepoData());

        assertThat(result.getScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("calculate() returns score = 0 when all components return 0 (critical)")
    void calculate_returnsZero_whenAllComponentsReturnZero() {
        stubComponents(0, 0, 0, 0);

        HealthScore result = calculator.calculate(anyRepoData());

        assertThat(result.getScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("calculate() correctly reflects partial scores in total")
    void calculate_correctlyReflectsPartialScores() {
        stubComponents(10, 5, 0, 7);   // total = 22

        HealthScore result = calculator.calculate(anyRepoData());

        assertThat(result.getScore()).isEqualTo(22);
    }

    // ── ScoreBreakdown accuracy ───────────────────────────────────────────────

    @Test
    @DisplayName("calculate() populates ScoreBreakdown with individual component scores")
    void calculate_populatesBreakdownWithIndividualScores() {
        stubComponents(20, 18, 22, 15);

        HealthScore result = calculator.calculate(anyRepoData());

        assertThat(result.getBreakdown().getCommitActivityScore()).isEqualTo(20);
        assertThat(result.getBreakdown().getIssueRatioScore()).isEqualTo(18);
        assertThat(result.getBreakdown().getCommunityScore()).isEqualTo(22);
        assertThat(result.getBreakdown().getDocumentationScore()).isEqualTo(15);
    }

    @Test
    @DisplayName("calculate() breakdown.getTotal() matches result.getScore()")
    void calculate_breakdownTotalMatchesScore() {
        stubComponents(15, 20, 18, 22);

        HealthScore result = calculator.calculate(anyRepoData());

        assertThat(result.getBreakdown().getTotal()).isEqualTo(result.getScore());
    }

    // ── Grade mapping ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Grade assignment")
    class GradeAssignment {

        @Test
        @DisplayName("calculate() assigns grade A for score >= 90")
        void calculate_assignsGradeA_forScore90AndAbove() {
            stubComponents(23, 23, 23, 22);  // total = 91

            HealthScore result = calculator.calculate(anyRepoData());

            assertThat(result.getScore()).isEqualTo(91);
            assertThat(result.getGrade()).isEqualTo("A");
        }

        @Test
        @DisplayName("calculate() assigns grade A for perfect score 100")
        void calculate_assignsGradeA_forPerfect100() {
            stubComponents(25, 25, 25, 25);

            assertThat(calculator.calculate(anyRepoData()).getGrade()).isEqualTo("A");
        }

        @Test
        @DisplayName("calculate() assigns grade B for score 75–89")
        void calculate_assignsGradeB_forScore75to89() {
            stubComponents(20, 18, 20, 17);  // total = 75

            HealthScore result = calculator.calculate(anyRepoData());

            assertThat(result.getScore()).isEqualTo(75);
            assertThat(result.getGrade()).isEqualTo("B");
        }

        @Test
        @DisplayName("calculate() assigns grade C for score 60–74")
        void calculate_assignsGradeC_forScore60to74() {
            stubComponents(16, 15, 15, 14);  // total = 60

            HealthScore result = calculator.calculate(anyRepoData());

            assertThat(result.getScore()).isEqualTo(60);
            assertThat(result.getGrade()).isEqualTo("C");
        }

        @Test
        @DisplayName("calculate() assigns grade D for score 40–59")
        void calculate_assignsGradeD_forScore40to59() {
            stubComponents(10, 10, 10, 10);  // total = 40

            HealthScore result = calculator.calculate(anyRepoData());

            assertThat(result.getScore()).isEqualTo(40);
            assertThat(result.getGrade()).isEqualTo("D");
        }

        @Test
        @DisplayName("calculate() assigns grade F for score < 40")
        void calculate_assignsGradeF_forScoreBelow40() {
            stubComponents(8, 8, 8, 8);  // total = 32

            HealthScore result = calculator.calculate(anyRepoData());

            assertThat(result.getScore()).isEqualTo(32);
            assertThat(result.getGrade()).isEqualTo("F");
        }

        @Test
        @DisplayName("calculate() assigns grade F for score = 0")
        void calculate_assignsGradeF_forZeroScore() {
            stubComponents(0, 0, 0, 0);

            assertThat(calculator.calculate(anyRepoData()).getGrade()).isEqualTo("F");
        }

        @Test
        @DisplayName("Grade boundary table: exact boundary values")
        void gradeBoundaries_exactValues() {
            // Verify all exact grade boundaries
            assertGradeForTotal(90, "A");
            assertGradeForTotal(89, "B");
            assertGradeForTotal(75, "B");
            assertGradeForTotal(74, "C");
            assertGradeForTotal(60, "C");
            assertGradeForTotal(59, "D");
            assertGradeForTotal(40, "D");
            assertGradeForTotal(39, "F");
            assertGradeForTotal(0,  "F");
        }
    }

    // ── Each component is called exactly once ─────────────────────────────────

    @Test
    @DisplayName("calculate() calls each ScoreComponent exactly once per invocation")
    void calculate_callsEachComponent_exactlyOnce() {
        stubComponents(20, 18, 22, 15);
        RepoData data = anyRepoData();

        calculator.calculate(data);

        verify(commitComponent,      times(1)).score(data);
        verify(issueComponent,       times(1)).score(data);
        verify(communityComponent,   times(1)).score(data);
        verify(documentationComponent, times(1)).score(data);
    }

    @Test
    @DisplayName("calculate() passes the same RepoData instance to every component")
    void calculate_passesSameRepoData_toAllComponents() {
        stubComponents(10, 10, 10, 10);
        RepoData data = anyRepoData();

        calculator.calculate(data);

        verify(commitComponent).score(same(data));
        verify(issueComponent).score(same(data));
        verify(communityComponent).score(same(data));
        verify(documentationComponent).score(same(data));
    }

    // ── Return value contract ────────────────────────────────────────────────

    @Test
    @DisplayName("calculate() never returns null")
    void calculate_neverReturnsNull() {
        stubComponents(20, 15, 18, 12);

        assertThat(calculator.calculate(anyRepoData())).isNotNull();
    }

    @Test
    @DisplayName("calculate() result always has a non-null, non-blank grade")
    void calculate_resultAlwaysHasGrade() {
        stubComponents(0, 0, 0, 0);

        HealthScore result = calculator.calculate(anyRepoData());

        assertThat(result.getGrade()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("calculate() result always has a non-null breakdown")
    void calculate_resultAlwaysHasBreakdown() {
        stubComponents(10, 10, 10, 10);

        assertThat(calculator.calculate(anyRepoData()).getBreakdown()).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Real components integration — no mocks
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Real components integration")
    class RealComponentsIntegration {

        private WeightedHealthScoreCalculator realCalculator;

        @BeforeEach
        void setUp() {
            realCalculator = new WeightedHealthScoreCalculator(
                    new CommitActivityScoreComponent(),
                    new IssueRatioScoreComponent(),
                    new CommunityEngagementScoreComponent(),
                    new DocumentationQualityScoreComponent(),
                    new ScoreGradeMapper());
        }

        @Test
        @DisplayName("Real: score is between 0 and 100 for any input")
        void real_scoreAlwaysInRange() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .stars(500).forks(50).openIssuesCount(25)
                            .description("A test repo").hasTopics(true).licenseId("MIT")
                            .build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(10)))
                    .rootContents(List.of(RepoDataTestFixtures.contentDir("test")))
                    .contributors(RepoDataTestFixtures.contributors(10))
                    .build();

            HealthScore result = realCalculator.calculate(data);

            assertThat(result.getScore()).isBetween(0, 100);
        }

        @Test
        @DisplayName("Real: healthy repo (recent commit, few issues, full docs) scores grade B or higher")
        void real_healthyRepo_scoresGradeBOrHigher() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .stars(5000).forks(800).openIssuesCount(10)
                            .description("A well-maintained library").hasTopics(true).licenseId("MIT")
                            .build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(2)))
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentFile("README.md"),
                            RepoDataTestFixtures.contentFile("LICENSE"),
                            RepoDataTestFixtures.contentDir("tests")
                    ))
                    .contributors(RepoDataTestFixtures.contributors(50))
                    .build();

            HealthScore result = realCalculator.calculate(data);

            assertThat(result.getScore()).isGreaterThanOrEqualTo(75);
            assertThat(result.getGrade()).isIn("A", "B");
        }

        @Test
        @DisplayName("Real: abandoned repo (180+ days, no docs, high issues) scores grade D or F")
        void real_abandonedRepo_scoresGradeDOrF() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .stars(5).forks(1).openIssuesCount(200)
                            .noDescription().hasTopics(false).noLicense()
                            .build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(365)))
                    .rootContents(List.of(RepoDataTestFixtures.contentFile("app.py")))
                    .contributors(RepoDataTestFixtures.contributors(1))
                    .build();

            HealthScore result = realCalculator.calculate(data);

            assertThat(result.getScore()).isLessThan(60);
            assertThat(result.getGrade()).isIn("D", "F");
        }

        @Test
        @DisplayName("Real: healthy repo scores higher than abandoned repo")
        void real_healthyRepoScoresHigher_thanAbandonedRepo() {
            RepoData healthy = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .stars(10000).forks(2000).openIssuesCount(5)
                            .description("Active library").hasTopics(true).licenseId("MIT")
                            .build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(1)))
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentFile("README.md"),
                            RepoDataTestFixtures.contentFile("LICENSE"),
                            RepoDataTestFixtures.contentDir("test")))
                    .contributors(RepoDataTestFixtures.contributors(20))
                    .build();

            RepoData abandoned = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .stars(3).forks(0).openIssuesCount(300)
                            .noDescription().noLicense().hasTopics(false)
                            .build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(500)))
                    .rootContents(List.of(RepoDataTestFixtures.contentFile("index.js")))
                    .contributors(RepoDataTestFixtures.contributors(1))
                    .build();

            int healthyScore   = realCalculator.calculate(healthy).getScore();
            int abandonedScore = realCalculator.calculate(abandoned).getScore();

            assertThat(healthyScore).isGreaterThan(abandonedScore);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void stubComponents(int commit, int issue, int community, int docs) {
        when(commitComponent.score(any())).thenReturn(commit);
        when(issueComponent.score(any())).thenReturn(issue);
        when(communityComponent.score(any())).thenReturn(community);
        when(documentationComponent.score(any())).thenReturn(docs);
    }

    private void assertGradeForTotal(int total, String expectedGrade) {
        // Distribute total across 4 components as evenly as possible
        int base = total / 4;
        int remainder = total % 4;
        stubComponents(base + (remainder > 0 ? 1 : 0),
                       base + (remainder > 1 ? 1 : 0),
                       base + (remainder > 2 ? 1 : 0),
                       base);

        HealthScore result = calculator.calculate(anyRepoData());
        assertThat(result.getGrade())
                .as("Expected grade %s for total score %d", expectedGrade, total)
                .isEqualTo(expectedGrade);
    }

    private RepoData anyRepoData() {
        return RepoDataTestFixtures.repoData().build();
    }
}
