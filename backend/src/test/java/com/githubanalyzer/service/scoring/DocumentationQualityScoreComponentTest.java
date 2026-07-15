package com.githubanalyzer.service.scoring;

import com.githubanalyzer.dto.github.GitHubContentDTO;
import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.scoring.component.DocumentationQualityScoreComponent;
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
 * Unit tests for {@link DocumentationQualityScoreComponent}.
 *
 * <p>Scoring logic under test:
 * <pre>
 *   +7  README present (file starting with "readme", case-insensitive)
 *   +7  LICENSE present (via license DTO field OR "license"/"licence" file in root)
 *   +6  Description present (non-blank string)
 *   +5  Has topics/tags set
 *   ─────────
 *   25  Maximum total
 * </pre>
 *
 * <p>Tests cover:
 * <ul>
 *   <li>All four signals present → 25 (max)</li>
 *   <li>No signals present → 0 (min)</li>
 *   <li>Each signal in isolation (+7, +7, +6, +5)</li>
 *   <li>README filename variants: README.md, README.rst, README.txt, Readme.md</li>
 *   <li>LICENSE detection via DTO field AND via file name</li>
 *   <li>LICENCE spelling (British) in file names</li>
 *   <li>Blank/whitespace-only description is treated as absent</li>
 *   <li>Null repoData / null repoDetails null-safety via BaseScoreComponent</li>
 *   <li>Empty root contents doesn't crash (no README / LICENSE from files)</li>
 * </ul>
 */
@DisplayName("DocumentationQualityScoreComponent")
class DocumentationQualityScoreComponentTest {

    private DocumentationQualityScoreComponent component;

    @BeforeEach
    void setUp() {
        component = new DocumentationQualityScoreComponent();
    }

    // ── Contract ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getName() returns 'documentation'")
    void getName_returnsCorrectKey() {
        assertThat(component.getName()).isEqualTo("documentation");
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

    // ── Extremes ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("score() returns 25 when all four documentation signals are present")
    void score_returns25_whenAllSignalsPresent() {
        RepoData data = RepoDataTestFixtures.repoData()
                .repoDetails(RepoDataTestFixtures.repo()
                        .licenseId("MIT")
                        .description("A great library")
                        .hasTopics(true)
                        .build())
                .rootContents(List.of(
                        RepoDataTestFixtures.contentFile("README.md"),
                        RepoDataTestFixtures.contentFile("LICENSE")
                ))
                .build();

        assertThat(component.score(data)).isEqualTo(25);
    }

    @Test
    @DisplayName("score() returns 0 when no documentation signals are present")
    void score_returnsZero_whenNoSignalsPresent() {
        RepoData data = RepoDataTestFixtures.repoData()
                .repoDetails(RepoDataTestFixtures.repo()
                        .noDescription()
                        .noLicense()
                        .hasTopics(false)
                        .build())
                .rootContents(Collections.emptyList())
                .build();

        assertThat(component.score(data)).isEqualTo(0);
    }

    // ── README detection (+7) ────────────────────────────────────────────────

    @Nested
    @DisplayName("README detection")
    class ReadmeDetection {

        @ParameterizedTest(name = "File ''{0}'' is recognised as a README")
        @ValueSource(strings = {
            "README.md",
            "README.rst",
            "README.txt",
            "README",
            "readme.md",          // lowercase
            "Readme.md",          // mixed case
            "README.adoc",        // AsciiDoc
            "readme"              // no extension
        })
        @DisplayName("score() adds 7 points for recognised README filename variants")
        void score_adds7_forReadmeVariants(String filename) {
            RepoData data = repoWithOnlyContents(List.of(
                    RepoDataTestFixtures.contentFile(filename)));

            assertThat(component.score(data)).isEqualTo(7);
        }

        @Test
        @DisplayName("score() does NOT add README points for 'read_me.txt' (not startsWith 'readme')")
        void score_doesNotAdd_forNonReadmeFile() {
            RepoData data = repoWithOnlyContents(List.of(
                    RepoDataTestFixtures.contentFile("read_me.txt")));

            assertThat(component.score(data)).isEqualTo(0);
        }

        @Test
        @DisplayName("score() adds 7 for README even when root contents have other files too")
        void score_addsReadmePoints_amidstOtherFiles() {
            RepoData data = repoWithOnlyContents(List.of(
                    RepoDataTestFixtures.contentFile("src"),
                    RepoDataTestFixtures.contentDir("docs"),
                    RepoDataTestFixtures.contentFile("README.md"),
                    RepoDataTestFixtures.contentFile("pom.xml")
            ));

            assertThat(component.score(data)).isEqualTo(7);
        }

        @Test
        @DisplayName("score() adds 0 for README when root contents is empty")
        void score_addsZeroReadme_whenContentsEmpty() {
            RepoData data = repoWithOnlyContents(Collections.emptyList());
            assertThat(component.score(data)).isEqualTo(0);
        }

        @Test
        @DisplayName("score() does not double-count if multiple README files present")
        void score_doesNotDoubleCount_multipleReadmes() {
            RepoData data = repoWithOnlyContents(List.of(
                    RepoDataTestFixtures.contentFile("README.md"),
                    RepoDataTestFixtures.contentFile("README.txt")
            ));

            assertThat(component.score(data)).isEqualTo(7); // still just 7, not 14
        }
    }

    // ── LICENSE detection (+7) ───────────────────────────────────────────────

    @Nested
    @DisplayName("LICENSE detection")
    class LicenseDetection {

        @Test
        @DisplayName("score() adds 7 when license field is set on the repo DTO")
        void score_adds7_whenLicenseDtoIsSet() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .licenseId("MIT")
                            .noDescription()
                            .hasTopics(false)
                            .build())
                    .rootContents(Collections.emptyList())
                    .build();

            assertThat(component.score(data)).isEqualTo(7);
        }

        @ParameterizedTest(name = "File ''{0}'' detected as LICENSE")
        @ValueSource(strings = {
            "LICENSE",
            "LICENSE.md",
            "LICENSE.txt",
            "license",            // lowercase
            "License.txt",        // mixed case
            "LICENCE",            // British spelling
            "LICENCE.md",
            "licence.txt"
        })
        @DisplayName("score() adds 7 for LICENSE/LICENCE file variants in root contents")
        void score_adds7_forLicenseFileVariants(String filename) {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .noLicense()      // no license DTO field
                            .noDescription()
                            .hasTopics(false)
                            .build())
                    .rootContents(List.of(RepoDataTestFixtures.contentFile(filename)))
                    .build();

            assertThat(component.score(data)).isEqualTo(7);
        }

        @Test
        @DisplayName("score() does not double-count when both DTO and file indicate license")
        void score_doesNotDoubleCount_licenseFromDtoAndFile() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .licenseId("MIT")
                            .noDescription()
                            .hasTopics(false)
                            .build())
                    .rootContents(List.of(RepoDataTestFixtures.contentFile("LICENSE")))
                    .build();

            assertThat(component.score(data)).isEqualTo(7); // 7, not 14
        }

        @Test
        @DisplayName("score() adds 0 for license when no DTO field and no LICENSE file")
        void score_addsZeroLicense_whenAbsent() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .noLicense()
                            .noDescription()
                            .hasTopics(false)
                            .build())
                    .rootContents(List.of(RepoDataTestFixtures.contentFile("README.md")))
                    .build();

            assertThat(component.score(data)).isEqualTo(7); // only README points
        }
    }

    // ── Description detection (+6) ───────────────────────────────────────────

    @Nested
    @DisplayName("Description detection")
    class DescriptionDetection {

        @Test
        @DisplayName("score() adds 6 when description is non-blank")
        void score_adds6_whenDescriptionIsPresent() {
            RepoData data = repoWithOnlyDescription("A useful library");
            assertThat(component.score(data)).isEqualTo(6);
        }

        @Test
        @DisplayName("score() adds 0 when description is null")
        void score_addsZero_whenDescriptionIsNull() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .noDescription()
                            .noLicense()
                            .hasTopics(false)
                            .build())
                    .rootContents(Collections.emptyList())
                    .build();

            assertThat(component.score(data)).isEqualTo(0);
        }

        @Test
        @DisplayName("score() adds 0 when description is blank whitespace")
        void score_addsZero_whenDescriptionIsBlank() {
            // StringUtils.hasText("   ") returns false
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .description("   ")
                            .noLicense()
                            .hasTopics(false)
                            .build())
                    .rootContents(Collections.emptyList())
                    .build();

            assertThat(component.score(data)).isEqualTo(0);
        }

        @Test
        @DisplayName("score() adds 0 when description is empty string")
        void score_addsZero_whenDescriptionIsEmpty() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .description("")
                            .noLicense()
                            .hasTopics(false)
                            .build())
                    .rootContents(Collections.emptyList())
                    .build();

            assertThat(component.score(data)).isEqualTo(0);
        }
    }

    // ── Topics detection (+5) ────────────────────────────────────────────────

    @Nested
    @DisplayName("Topics detection")
    class TopicsDetection {

        @Test
        @DisplayName("score() adds 5 when hasTopics is true")
        void score_adds5_whenHasTopicsIsTrue() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .noDescription()
                            .noLicense()
                            .hasTopics(true)
                            .build())
                    .rootContents(Collections.emptyList())
                    .build();

            assertThat(component.score(data)).isEqualTo(5);
        }

        @Test
        @DisplayName("score() adds 0 when hasTopics is false")
        void score_addsZero_whenHasTopicsIsFalse() {
            RepoData data = RepoDataTestFixtures.repoData()
                    .repoDetails(RepoDataTestFixtures.repo()
                            .noDescription()
                            .noLicense()
                            .hasTopics(false)
                            .build())
                    .rootContents(Collections.emptyList())
                    .build();

            assertThat(component.score(data)).isEqualTo(0);
        }
    }

    // ── Additive scoring verification ────────────────────────────────────────

    @Test
    @DisplayName("score() correctly adds README(7) + description(6) = 13 when only those are present")
    void score_correctlyAdds_readmeAndDescription() {
        RepoData data = RepoDataTestFixtures.repoData()
                .repoDetails(RepoDataTestFixtures.repo()
                        .description("A test repo")
                        .noLicense()
                        .hasTopics(false)
                        .build())
                .rootContents(List.of(RepoDataTestFixtures.contentFile("README.md")))
                .build();

        assertThat(component.score(data)).isEqualTo(13); // 7 + 6
    }

    @Test
    @DisplayName("score() correctly adds LICENSE(7) + topics(5) = 12 when only those are present")
    void score_correctlyAdds_licenseAndTopics() {
        RepoData data = RepoDataTestFixtures.repoData()
                .repoDetails(RepoDataTestFixtures.repo()
                        .licenseId("Apache-2.0")
                        .noDescription()
                        .hasTopics(true)
                        .build())
                .rootContents(Collections.emptyList())
                .build();

        assertThat(component.score(data)).isEqualTo(12); // 7 + 5
    }

    @Test
    @DisplayName("score() correctly adds README(7) + LICENSE(7) + description(6) = 20 (no topics)")
    void score_correctlyAdds_readmeLicenseDescription() {
        RepoData data = RepoDataTestFixtures.repoData()
                .repoDetails(RepoDataTestFixtures.repo()
                        .licenseId("MIT")
                        .description("A library")
                        .hasTopics(false)
                        .build())
                .rootContents(List.of(RepoDataTestFixtures.contentFile("README.md")))
                .build();

        assertThat(component.score(data)).isEqualTo(20); // 7 + 7 + 6
    }

    // ── Content file with null name (defensive) ───────────────────────────────

    @Test
    @DisplayName("score() handles content entry with null name without throwing")
    void score_handlesNullContentNameGracefully() {
        GitHubContentDTO nullNameEntry = new GitHubContentDTO();
        nullNameEntry.setType("file");
        // name is deliberately left null

        RepoData data = repoWithOnlyContents(List.of(nullNameEntry));

        // Should not throw — the filter(name != null) in the implementation guards this
        assertThat(component.score(data)).isEqualTo(0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RepoData repoWithOnlyContents(List<GitHubContentDTO> contents) {
        return RepoDataTestFixtures.repoData()
                .repoDetails(RepoDataTestFixtures.repo()
                        .noDescription()
                        .noLicense()
                        .hasTopics(false)
                        .build())
                .rootContents(contents)
                .build();
    }

    private RepoData repoWithOnlyDescription(String description) {
        return RepoDataTestFixtures.repoData()
                .repoDetails(RepoDataTestFixtures.repo()
                        .description(description)
                        .noLicense()
                        .hasTopics(false)
                        .build())
                .rootContents(Collections.emptyList())
                .build();
    }
}
