package com.githubanalyzer.service.alert;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.domain.enums.AlertType;
import com.githubanalyzer.domain.enums.Severity;
import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.alert.impl.RuleBasedAlertEngine;
import com.githubanalyzer.service.alert.rule.*;
import com.githubanalyzer.util.RepoDataTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RuleBasedAlertEngine}.
 *
 * <p>Two test approaches are used:
 * <ol>
 *   <li><b>Mocked rules</b> — verifies the engine's orchestration logic in isolation:
 *       does it call all rules, collect all results, handle empty lists, etc.</li>
 *   <li><b>Real rules</b> — integration-style tests that verify the engine correctly
 *       wires together the four production rules against realistic data scenarios.</li>
 * </ol>
 *
 * <p>Key properties verified:
 * <ul>
 *   <li>Zero rules → empty alert list (no NPE)</li>
 *   <li>All rules evaluated independently — multiple alerts can fire together</li>
 *   <li>Rules that don't apply are filtered out correctly</li>
 *   <li>A rule that applies always has generateAlert() called exactly once</li>
 *   <li>Alert list is ordered by rule registration order</li>
 *   <li>All four production rules fire together for a worst-case repository</li>
 *   <li>No alerts fire for a perfectly healthy repository</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleBasedAlertEngine")
class RuleBasedAlertEngineTest {

    // ── Shared alert fixtures ─────────────────────────────────────────────────

    private static final Alert INACTIVE_ALERT =
            Alert.of(AlertType.INACTIVE_REPO, "Repository appears inactive", Severity.WARNING);
    private static final Alert HIGH_ISSUES_ALERT =
            Alert.of(AlertType.HIGH_ISSUE_BACKLOG, "High issue backlog - Maintenance concern", Severity.WARNING);
    private static final Alert NO_TESTS_ALERT =
            Alert.of(AlertType.NO_TESTS, "Testing infrastructure not found", Severity.WARNING);
    private static final Alert SECURITY_ALERT =
            Alert.of(AlertType.SECURITY_VULNERABILITIES, "Security vulnerabilities detected - Review dependencies", Severity.CRITICAL);

    // ════════════════════════════════════════════════════════════════════════
    // Part 1: Engine orchestration tests (mocked rules)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Engine orchestration (mocked rules)")
    class EngineOrchestration {

        @Mock private AlertRule mockRuleA;
        @Mock private AlertRule mockRuleB;
        @Mock private AlertRule mockRuleC;

        private RepoData anyData;

        @BeforeEach
        void setUp() {
            anyData = RepoDataTestFixtures.repoData().build();
        }

        @Test
        @DisplayName("evaluate() returns empty list when no rules are registered")
        void evaluate_returnsEmpty_whenNoRulesRegistered() {
            RuleBasedAlertEngine engine = new RuleBasedAlertEngine(Collections.emptyList());

            List<Alert> alerts = engine.evaluate(anyData);

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("evaluate() returns empty list when all rules return applies=false")
        void evaluate_returnsEmpty_whenNoRuleApplies() {
            when(mockRuleA.applies(any())).thenReturn(false);
            when(mockRuleB.applies(any())).thenReturn(false);

            RuleBasedAlertEngine engine = new RuleBasedAlertEngine(List.of(mockRuleA, mockRuleB));
            List<Alert> alerts = engine.evaluate(anyData);

            assertThat(alerts).isEmpty();
            // generateAlert must NOT be called when applies() is false
            verify(mockRuleA, never()).generateAlert(any());
            verify(mockRuleB, never()).generateAlert(any());
        }

        @Test
        @DisplayName("evaluate() calls every registered rule's applies() exactly once")
        void evaluate_callsAppliesOnce_forEveryRule() {
            when(mockRuleA.applies(any())).thenReturn(false);
            when(mockRuleB.applies(any())).thenReturn(false);
            when(mockRuleC.applies(any())).thenReturn(false);

            RuleBasedAlertEngine engine = new RuleBasedAlertEngine(
                    List.of(mockRuleA, mockRuleB, mockRuleC));
            engine.evaluate(anyData);

            verify(mockRuleA, times(1)).applies(anyData);
            verify(mockRuleB, times(1)).applies(anyData);
            verify(mockRuleC, times(1)).applies(anyData);
        }

        @Test
        @DisplayName("evaluate() returns exactly one alert when only one rule applies")
        void evaluate_returnsOneAlert_whenOneRuleApplies() {
            when(mockRuleA.applies(any())).thenReturn(true);
            when(mockRuleA.generateAlert(any())).thenReturn(INACTIVE_ALERT);
            when(mockRuleB.applies(any())).thenReturn(false);

            RuleBasedAlertEngine engine = new RuleBasedAlertEngine(List.of(mockRuleA, mockRuleB));
            List<Alert> alerts = engine.evaluate(anyData);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getType()).isEqualTo(AlertType.INACTIVE_REPO);
        }

        @Test
        @DisplayName("evaluate() returns all alerts when all rules apply (all fire together)")
        void evaluate_returnsAllAlerts_whenAllRulesApply() {
            when(mockRuleA.applies(any())).thenReturn(true);
            when(mockRuleA.generateAlert(any())).thenReturn(INACTIVE_ALERT);
            when(mockRuleB.applies(any())).thenReturn(true);
            when(mockRuleB.generateAlert(any())).thenReturn(HIGH_ISSUES_ALERT);
            when(mockRuleC.applies(any())).thenReturn(true);
            when(mockRuleC.generateAlert(any())).thenReturn(NO_TESTS_ALERT);

            RuleBasedAlertEngine engine = new RuleBasedAlertEngine(
                    List.of(mockRuleA, mockRuleB, mockRuleC));
            List<Alert> alerts = engine.evaluate(anyData);

            assertThat(alerts).hasSize(3);
            assertThat(alerts).extracting(Alert::getType)
                    .containsExactlyInAnyOrder(
                            AlertType.INACTIVE_REPO,
                            AlertType.HIGH_ISSUE_BACKLOG,
                            AlertType.NO_TESTS);
        }

        @Test
        @DisplayName("evaluate() calls generateAlert() exactly once per applicable rule")
        void evaluate_callsGenerateAlert_exactlyOnce_perApplicableRule() {
            when(mockRuleA.applies(any())).thenReturn(true);
            when(mockRuleA.generateAlert(any())).thenReturn(INACTIVE_ALERT);
            when(mockRuleB.applies(any())).thenReturn(false);

            RuleBasedAlertEngine engine = new RuleBasedAlertEngine(List.of(mockRuleA, mockRuleB));
            engine.evaluate(anyData);

            verify(mockRuleA, times(1)).generateAlert(anyData);
            verify(mockRuleB, never()).generateAlert(any()); // non-applicable rule never called
        }

        @Test
        @DisplayName("evaluate() preserves rule registration order in the returned alert list")
        void evaluate_preservesOrder_ofRuleRegistration() {
            when(mockRuleA.applies(any())).thenReturn(true);
            when(mockRuleA.generateAlert(any())).thenReturn(INACTIVE_ALERT);
            when(mockRuleB.applies(any())).thenReturn(true);
            when(mockRuleB.generateAlert(any())).thenReturn(HIGH_ISSUES_ALERT);
            when(mockRuleC.applies(any())).thenReturn(true);
            when(mockRuleC.generateAlert(any())).thenReturn(NO_TESTS_ALERT);

            // Register in specific order: A, B, C
            RuleBasedAlertEngine engine = new RuleBasedAlertEngine(
                    List.of(mockRuleA, mockRuleB, mockRuleC));
            List<Alert> alerts = engine.evaluate(anyData);

            // Order must match rule registration order
            assertThat(alerts).hasSize(3);
            assertThat(alerts.get(0).getType()).isEqualTo(AlertType.INACTIVE_REPO);
            assertThat(alerts.get(1).getType()).isEqualTo(AlertType.HIGH_ISSUE_BACKLOG);
            assertThat(alerts.get(2).getType()).isEqualTo(AlertType.NO_TESTS);
        }

        @Test
        @DisplayName("evaluate() passes the same RepoData instance to every rule")
        void evaluate_passesSameRepoDataInstance_toAllRules() {
            when(mockRuleA.applies(any())).thenReturn(false);
            when(mockRuleB.applies(any())).thenReturn(false);

            RuleBasedAlertEngine engine = new RuleBasedAlertEngine(List.of(mockRuleA, mockRuleB));
            engine.evaluate(anyData);

            // Both rules must receive the exact same RepoData reference
            verify(mockRuleA).applies(same(anyData));
            verify(mockRuleB).applies(same(anyData));
        }

        @Test
        @DisplayName("evaluate() skips a rule whose applies() is false even if next rule fires")
        void evaluate_skipsFalseRule_evenWhenSubsequentRuleFires() {
            when(mockRuleA.applies(any())).thenReturn(false); // skip
            when(mockRuleB.applies(any())).thenReturn(true);  // fire
            when(mockRuleB.generateAlert(any())).thenReturn(HIGH_ISSUES_ALERT);

            RuleBasedAlertEngine engine = new RuleBasedAlertEngine(List.of(mockRuleA, mockRuleB));
            List<Alert> alerts = engine.evaluate(anyData);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getType()).isEqualTo(AlertType.HIGH_ISSUE_BACKLOG);
            verify(mockRuleA, never()).generateAlert(any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Part 2: Real rules integration tests
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Real rules integration")
    class RealRulesIntegration {

        private RuleBasedAlertEngine engine;

        @BeforeEach
        void setUp() {
            // Wire all four production rules, same as Spring would do
            engine = new RuleBasedAlertEngine(List.of(
                    new InactiveRepoRule(),
                    new HighIssueBacklogRule(),
                    new NoTestInfrastructureRule(),
                    new SecurityVulnerabilityRule()
            ));
        }

        // ── Zero alerts: healthy repository ──────────────────────────────────

        @Test
        @DisplayName("evaluate() returns empty list for a perfectly healthy repository")
        void evaluate_returnsNoAlerts_forHealthyRepo() {
            // Recent commit, few issues, has tests, no security files
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(10).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(3)))
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentFile("README.md"),
                            RepoDataTestFixtures.contentDir("test"),        // tests present
                            RepoDataTestFixtures.contentFile("pom.xml")
                    ))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts).isEmpty();
        }

        // ── Single alerts ─────────────────────────────────────────────────────

        @Test
        @DisplayName("evaluate() fires only INACTIVE_REPO alert for stale repo with otherwise clean state")
        void evaluate_firesOnlyInactiveAlert_forStaleRepo() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(5).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(200))) // inactive
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentDir("test"),
                            RepoDataTestFixtures.contentFile("README.md")
                    ))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getType()).isEqualTo(AlertType.INACTIVE_REPO);
        }

        @Test
        @DisplayName("evaluate() fires only HIGH_ISSUE_BACKLOG alert for repo with 51 open issues")
        void evaluate_firesOnlyHighIssueAlert_for51Issues() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(51).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(5)))   // active
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentDir("tests"),
                            RepoDataTestFixtures.contentFile("package.json")
                    ))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getType()).isEqualTo(AlertType.HIGH_ISSUE_BACKLOG);
        }

        @Test
        @DisplayName("evaluate() fires only NO_TESTS alert for active repo with no test files")
        void evaluate_firesOnlyNoTestsAlert_forActiveRepoWithNoTests() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(5).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(5)))
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentFile("README.md"),  // no test dir/file
                            RepoDataTestFixtures.contentFile("pom.xml")
                    ))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getType()).isEqualTo(AlertType.NO_TESTS);
        }

        @Test
        @DisplayName("evaluate() fires only SECURITY_VULNERABILITIES alert for committed .env")
        void evaluate_firesOnlySecurityAlert_forCommittedDotEnv() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(5).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(5)))
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentFile(".env"),        // ← risk file
                            RepoDataTestFixtures.contentDir("test"),
                            RepoDataTestFixtures.contentFile("README.md")
                    ))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getType()).isEqualTo(AlertType.SECURITY_VULNERABILITIES);
            assertThat(alerts.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
        }

        // ── Multiple alerts firing together ───────────────────────────────────

        @Test
        @DisplayName("evaluate() fires both INACTIVE_REPO and HIGH_ISSUE_BACKLOG for dead repo with backlog")
        void evaluate_firesBothInactiveAndHighIssue() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(100).build()) // high issues
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(180)))        // inactive
                    .rootContents(List.of(RepoDataTestFixtures.contentDir("test")))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts).hasSize(2);
            assertThat(alerts).extracting(Alert::getType)
                    .containsExactlyInAnyOrder(
                            AlertType.INACTIVE_REPO,
                            AlertType.HIGH_ISSUE_BACKLOG);
        }

        @Test
        @DisplayName("evaluate() fires INACTIVE_REPO + NO_TESTS for stale repo without tests")
        void evaluate_firesInactiveAndNoTests_forStaleRepoWithoutTests() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(5).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(120)))   // inactive
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentFile("README.md"),
                            RepoDataTestFixtures.contentFile("main.py")               // no tests
                    ))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts).hasSize(2);
            assertThat(alerts).extracting(Alert::getType)
                    .containsExactlyInAnyOrder(
                            AlertType.INACTIVE_REPO,
                            AlertType.NO_TESTS);
        }

        @Test
        @DisplayName("evaluate() fires all four alerts for a worst-case repository")
        void evaluate_firesAllFourAlerts_forWorstCaseRepository() {
            // A truly problematic repository:
            // - Abandoned (200+ days no commit)
            // - Massive issue backlog (1000+ issues)
            // - No test infrastructure found in root contents
            // - Committed .env file (secrets leak)
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(1000).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(365)))
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentFile("README.md"),
                            RepoDataTestFixtures.contentFile("app.py"),
                            RepoDataTestFixtures.contentFile(".env")     // committed secret
                            // no test dir/file
                    ))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts).hasSize(4);
            assertThat(alerts).extracting(Alert::getType)
                    .containsExactlyInAnyOrder(
                            AlertType.INACTIVE_REPO,
                            AlertType.HIGH_ISSUE_BACKLOG,
                            AlertType.NO_TESTS,
                            AlertType.SECURITY_VULNERABILITIES);
        }

        // ── Boundary conditions with real rules ───────────────────────────────

        @Test
        @DisplayName("evaluate() fires 0 alerts at all thresholds (exactly 90 days, exactly 50 issues)")
        void evaluate_firesNoAlerts_atExactThresholds() {
            // 90 days → NOT inactive (rule uses > 90), 50 issues → NOT backlog (rule uses > 50)
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(50).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(90)))
                    .rootContents(List.of(RepoDataTestFixtures.contentDir("test")))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts)
                    .as("At the exact threshold values, no alert should fire")
                    .isEmpty();
        }

        @Test
        @DisplayName("evaluate() fires both threshold alerts at threshold+1 (91 days, 51 issues)")
        void evaluate_firesBothThresholdAlerts_atThresholdPlusOne() {
            // 91 days → inactive, 51 issues → high backlog
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(51).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(91)))
                    .rootContents(List.of(RepoDataTestFixtures.contentDir("test")))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts).hasSize(2);
            assertThat(alerts).extracting(Alert::getType)
                    .containsExactlyInAnyOrder(
                            AlertType.INACTIVE_REPO,
                            AlertType.HIGH_ISSUE_BACKLOG);
        }

        @Test
        @DisplayName("evaluate() does not fire INACTIVE or HIGH_ISSUE when data is missing/empty")
        void evaluate_doesNotFireDataDependentAlerts_whenDataIsAbsent() {
            // Empty commits → InactiveRepoRule returns false (can't confirm inactivity)
            // rootContents empty → NoTestInfrastructureRule returns false (can't confirm absence)
            // rootContents empty → SecurityVulnerabilityRule returns false (nothing to scan)
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(0).build())
                    .recentCommits(Collections.emptyList())
                    .rootContents(Collections.emptyList())
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            // Without data, no alert should fire (safety by silence)
            assertThat(alerts).isEmpty();
        }

        // ── Alert severity validation across all alerts ───────────────────────

        @Test
        @DisplayName("evaluate() produces only WARNING severity for non-security alerts")
        void evaluate_allNonSecurityAlerts_areWarning() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(1000).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(365)))
                    .rootContents(List.of(RepoDataTestFixtures.contentFile("README.md")))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            // Only non-security alerts should fire here (no .env file)
            assertThat(alerts).extracting(Alert::getSeverity)
                    .allMatch(s -> s == Severity.WARNING);
        }

        @Test
        @DisplayName("evaluate() SECURITY alert is CRITICAL while others remain WARNING")
        void evaluate_securityAlertIsCritical_othersAreWarning() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(100).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(200)))
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentFile("README.md"),
                            RepoDataTestFixtures.contentFile(".env")   // triggers CRITICAL
                    ))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            long criticalCount = alerts.stream()
                    .filter(a -> a.getSeverity() == Severity.CRITICAL).count();
            long warningCount = alerts.stream()
                    .filter(a -> a.getSeverity() == Severity.WARNING).count();

            assertThat(criticalCount).isEqualTo(1);  // security only
            assertThat(warningCount).isGreaterThanOrEqualTo(1);  // inactive + high issues
        }

        // ── Problem statement message alignment ───────────────────────────────

        @Test
        @DisplayName("All four alert messages match the problem statement exactly")
        void allAlertMessages_matchProblemStatementExactly() {
            // Fire all four alerts and verify their messages
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo().openIssuesCount(1000).build())
                    .recentCommits(List.of(RepoDataTestFixtures.commitDaysAgo(365)))
                    .rootContents(List.of(
                            RepoDataTestFixtures.contentFile("README.md"),
                            RepoDataTestFixtures.contentFile(".env")
                    ))
                    .build();

            List<Alert> alerts = engine.evaluate(data);

            assertThat(alerts).extracting(Alert::getMessage)
                    .containsExactlyInAnyOrder(
                            "Repository appears inactive",
                            "High issue backlog - Maintenance concern",
                            "Testing infrastructure not found",
                            "Security vulnerabilities detected - Review dependencies"
                    );
        }
    }
}
