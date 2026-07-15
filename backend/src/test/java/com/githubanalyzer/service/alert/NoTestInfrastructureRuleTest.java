package com.githubanalyzer.service.alert;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.domain.enums.AlertType;
import com.githubanalyzer.domain.enums.Severity;
import com.githubanalyzer.dto.github.GitHubContentDTO;
import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.alert.rule.NoTestInfrastructureRule;
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
 * Unit tests for {@link NoTestInfrastructureRule}.
 *
 * <p>Rule fires when:
 * <ol>
 *   <li>Root contents list is NOT empty (we actually fetched files)</li>
 *   <li>AND none of the entries look like test infrastructure</li>
 * </ol>
 *
 * <p>Rule does NOT fire when:
 * <ul>
 *   <li>Root contents is empty (API call failed — we don't alert on missing data)</li>
 *   <li>At least one entry matches a known test naming pattern</li>
 * </ul>
 *
 * <p>Test naming patterns recognised (via {@code GitHubContentDTO.looksLikeTestEntry()}):
 * <ul>
 *   <li>Directories: {@code test}, {@code tests}, {@code __tests__}, {@code spec}, {@code specs}</li>
 *   <li>Files: {@code *.test.ts}, {@code *.test.js}, {@code *.spec.ts}, {@code *.spec.js},
 *              {@code *Test.java}, {@code *Tests.java}, {@code test_*.py}, {@code *_test.py}</li>
 * </ul>
 */
@DisplayName("NoTestInfrastructureRule")
class NoTestInfrastructureRuleTest {

    private NoTestInfrastructureRule rule;

    @BeforeEach
    void setUp() {
        rule = new NoTestInfrastructureRule();
    }

    // ── Rule does NOT apply — empty contents ─────────────────────────────────

    @Test
    @DisplayName("applies() returns false when root contents is empty (API call failed)")
    void applies_returnsFalse_whenRootContentsIsEmpty() {
        // Trust boundary: absence of data ≠ absence of tests
        RepoData data = RepoDataTestFixtures.repoData()
                .rootContents(Collections.emptyList())
                .build();

        assertThat(rule.applies(data)).isFalse();
    }

    // ── Rule does NOT apply — test files detected ─────────────────────────────

    @Nested
    @DisplayName("applies() returns false — test infrastructure IS present")
    class TestInfrastructurePresent {

        @ParameterizedTest(name = "Directory ''{0}'' suppresses the alert")
        @ValueSource(strings = {"test", "tests", "__tests__", "spec", "specs"})
        @DisplayName("applies() returns false for recognised test directory names")
        void applies_returnsFalse_forTestDirectories(String dirName) {
            RepoData data = repoWithContents(List.of(
                    RepoDataTestFixtures.contentDir(dirName)
            ));
            assertThat(rule.applies(data)).isFalse();
        }

        @ParameterizedTest(name = "File ''{0}'' suppresses the alert")
        @ValueSource(strings = {
            "App.test.ts",
            "App.test.js",
            "App.spec.ts",
            "App.spec.js",
            "UserServiceTest.java",
            "UserServiceTests.java",
            "test_utils.py",
            "auth_test.py"
        })
        @DisplayName("applies() returns false for recognised test file suffixes")
        void applies_returnsFalse_forTestFileSuffixes(String fileName) {
            RepoData data = repoWithContents(List.of(
                    RepoDataTestFixtures.contentFile(fileName)
            ));
            assertThat(rule.applies(data)).isFalse();
        }

        @Test
        @DisplayName("applies() returns false when test dir is mixed with many other files")
        void applies_returnsFalse_testDirAmidstOtherFiles() {
            RepoData data = repoWithContents(List.of(
                    RepoDataTestFixtures.contentDir("src"),
                    RepoDataTestFixtures.contentFile("README.md"),
                    RepoDataTestFixtures.contentFile("pom.xml"),
                    RepoDataTestFixtures.contentDir("test"),     // ← present
                    RepoDataTestFixtures.contentFile(".gitignore")
            ));
            assertThat(rule.applies(data)).isFalse();
        }

        @Test
        @DisplayName("applies() returns false when __tests__ directory is present (Jest convention)")
        void applies_returnsFalse_jestTestsDirectory() {
            RepoData data = repoWithContents(List.of(
                    RepoDataTestFixtures.contentDir("src"),
                    RepoDataTestFixtures.contentDir("__tests__"),
                    RepoDataTestFixtures.contentFile("package.json")
            ));
            assertThat(rule.applies(data)).isFalse();
        }

        @Test
        @DisplayName("applies() returns false for Java project with 'src' containing test classes")
        void applies_returnsFalse_javaTestFileSuffix() {
            // A Java project where test classes appear at the root scan level
            RepoData data = repoWithContents(List.of(
                    RepoDataTestFixtures.contentDir("src"),
                    RepoDataTestFixtures.contentFile("CalculatorTest.java"),
                    RepoDataTestFixtures.contentFile("pom.xml")
            ));
            assertThat(rule.applies(data)).isFalse();
        }
    }

    // ── Rule DOES apply — no test infrastructure ─────────────────────────────

    @Nested
    @DisplayName("applies() returns true — NO test infrastructure found")
    class NoTestInfrastructureFound {

        @Test
        @DisplayName("applies() returns true when root has files but none are test-related")
        void applies_returnsTrue_noTestFilesAtAll() {
            RepoData data = repoWithContents(List.of(
                    RepoDataTestFixtures.contentFile("README.md"),
                    RepoDataTestFixtures.contentFile("pom.xml"),
                    RepoDataTestFixtures.contentDir("src"),
                    RepoDataTestFixtures.contentFile(".gitignore")
            ));
            assertThat(rule.applies(data)).isTrue();
        }

        @Test
        @DisplayName("applies() returns true for a single non-test file in root")
        void applies_returnsTrue_singleNonTestFile() {
            RepoData data = repoWithContents(List.of(
                    RepoDataTestFixtures.contentFile("index.js")
            ));
            assertThat(rule.applies(data)).isTrue();
        }

        @Test
        @DisplayName("applies() returns true when file name 'testing.md' does not match pattern")
        void applies_returnsTrue_fileNamedTestingMd() {
            // 'testing.md' does NOT start with 'test' as an exact dir name
            // nor match any suffix pattern — so the rule fires
            RepoData data = repoWithContents(List.of(
                    RepoDataTestFixtures.contentFile("testing.md"),
                    RepoDataTestFixtures.contentFile("README.md")
            ));
            // 'testing.md'.equals("test") → false, not a recognised pattern
            // However, 'testing.md'.startsWith("test_") → false
            // So this should fire
            assertThat(rule.applies(data)).isTrue();
        }

        @Test
        @DisplayName("applies() returns true for a project with only configuration files")
        void applies_returnsTrue_configOnlyProject() {
            RepoData data = repoWithContents(List.of(
                    RepoDataTestFixtures.contentFile("package.json"),
                    RepoDataTestFixtures.contentFile("tsconfig.json"),
                    RepoDataTestFixtures.contentFile(".eslintrc.json"),
                    RepoDataTestFixtures.contentDir("node_modules")
            ));
            assertThat(rule.applies(data)).isTrue();
        }

        @ParameterizedTest(name = "File ''{0}'' does NOT match test pattern → rule fires")
        @ValueSource(strings = {
            "testable.js",        // starts with 'test' but doesn't match any pattern exactly
            "contest.py",         // contains 'test' but doesn't match
            "protestant.md",      // ends with 'ant'
            "test.md",            // 'test.md' — NOT equal to 'test' directory
            "SPEC.md"             // uppercase, not lowercase 'spec'
        })
        @DisplayName("applies() returns true for files that look like test but don't match any pattern")
        void applies_returnsTrue_forNearMisses(String fileName) {
            RepoData data = repoWithContents(List.of(
                    RepoDataTestFixtures.contentFile(fileName)
            ));
            // These should all fire because looksLikeTestEntry() returns false for them
            // (patterns are exact: equals("test"), equals("tests"), etc.)
            assertThat(rule.applies(data))
                    .as("File '%s' should not match test pattern, so rule should apply", fileName)
                    .isTrue();
        }
    }

    // ── Null entry name handling ──────────────────────────────────────────────

    @Test
    @DisplayName("applies() handles a content entry with null name without throwing")
    void applies_handlesNullNameEntry_gracefully() {
        GitHubContentDTO nullNameEntry = new GitHubContentDTO();
        nullNameEntry.setType("file");
        // name intentionally left null

        RepoData data = repoWithContents(List.of(nullNameEntry));

        // Should not throw — looksLikeTestEntry() has a null guard: if (name == null) return false
        assertThat(rule.applies(data)).isTrue(); // null-name entry doesn't suppress the alert
    }

    @Test
    @DisplayName("applies() handles mix of null-named and real entries")
    void applies_handlesMixedNullAndRealEntries() {
        GitHubContentDTO nullNameEntry = new GitHubContentDTO();
        nullNameEntry.setType("dir");

        RepoData data = repoWithContents(List.of(
                nullNameEntry,
                RepoDataTestFixtures.contentDir("test")  // valid test entry
        ));

        // Despite the null entry, the 'test' dir should suppress the alert
        assertThat(rule.applies(data)).isFalse();
    }

    // ── Alert content verification ────────────────────────────────────────────

    @Test
    @DisplayName("generateAlert() produces NO_TESTS alert type")
    void generateAlert_producesCorrectType() {
        RepoData data = repoWithContents(List.of(
                RepoDataTestFixtures.contentFile("README.md")
        ));
        Alert alert = rule.generateAlert(data);

        assertThat(alert.getType()).isEqualTo(AlertType.NO_TESTS);
    }

    @Test
    @DisplayName("generateAlert() produces WARNING severity")
    void generateAlert_producesWarningSeverity() {
        RepoData data = repoWithContents(List.of(
                RepoDataTestFixtures.contentFile("README.md")
        ));
        Alert alert = rule.generateAlert(data);

        assertThat(alert.getSeverity()).isEqualTo(Severity.WARNING);
    }

    @Test
    @DisplayName("generateAlert() message matches problem statement exactly")
    void generateAlert_messageMatchesProblemStatement() {
        RepoData data = repoWithContents(List.of(
                RepoDataTestFixtures.contentFile("README.md")
        ));
        Alert alert = rule.generateAlert(data);

        assertThat(alert.getMessage()).isEqualTo("Testing infrastructure not found");
    }

    // ── Trust boundary: not exhaustive ───────────────────────────────────────

    @Test
    @DisplayName("applies() fires even when 'src/test' would exist but is not visible in root scan")
    void trustBoundary_cannotSeeNestedTestDirs() {
        // The rule only sees root-level contents. A Java project with src/test/java
        // would not have 'test' at root — so the rule fires even though tests exist.
        // This is an acknowledged trust boundary, not a bug.
        RepoData data = repoWithContents(List.of(
                RepoDataTestFixtures.contentDir("src"),     // contains test/ inside
                RepoDataTestFixtures.contentFile("pom.xml")
        ));
        // 'src' is not 'test', so the rule fires
        assertThat(rule.applies(data)).isTrue();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private RepoData repoWithContents(List<GitHubContentDTO> contents) {
        return RepoDataTestFixtures.repoData()
                .rootContents(contents)
                .build();
    }
}
