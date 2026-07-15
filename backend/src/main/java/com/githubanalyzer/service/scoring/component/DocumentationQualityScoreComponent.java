package com.githubanalyzer.service.scoring.component;

import com.githubanalyzer.dto.github.GitHubContentDTO;
import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Scores the repository based on documentation and metadata quality.
 *
 * <p><b>Scoring Logic (max 25 points):</b>
 * <ul>
 *   <li>+7  Has a README file (README.md, README.rst, README.txt, etc.)</li>
 *   <li>+7  Has a LICENSE file</li>
 *   <li>+6  Has a non-empty description set on the repository</li>
 *   <li>+5  Has topics/tags set (indicates community discoverability)</li>
 * </ul>
 *
 * <p>Total maximum: 25 points.
 *
 * <p>README and LICENSE detection is based on root contents from
 * {@code GET /repos/{owner}/{repo}/contents}. Description and topics
 * are sourced from the main repo DTO.
 */
@Component
public class DocumentationQualityScoreComponent extends BaseScoreComponent {

    private static final int MAX_SCORE = 25;

    private static final int README_POINTS = 7;
    private static final int LICENSE_POINTS = 7;
    private static final int DESCRIPTION_POINTS = 6;
    private static final int TOPICS_POINTS = 5;

    @Override
    protected int computeRawScore(RepoData repoData) {
        int score = 0;

        if (hasReadme(repoData)) score += README_POINTS;
        if (hasLicense(repoData)) score += LICENSE_POINTS;
        if (hasDescription(repoData)) score += DESCRIPTION_POINTS;
        if (hasTopics(repoData)) score += TOPICS_POINTS;

        return score;
    }

    @Override
    public int getMaxScore() {
        return MAX_SCORE;
    }

    @Override
    public String getName() {
        return "documentation";
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private boolean hasReadme(RepoData repoData) {
        // Check root contents for README variants
        return repoData.getRootContents().stream()
                .map(GitHubContentDTO::getName)
                .filter(name -> name != null)
                .anyMatch(name -> name.toLowerCase().startsWith("readme"));
    }

    private boolean hasLicense(RepoData repoData) {
        // Check via the license field on the repo DTO
        if (StringUtils.hasText(repoData.getRepoDetails().getLicenseName())) {
            return true;
        }
        // Also check root contents for a LICENSE file
        return repoData.getRootContents().stream()
                .map(GitHubContentDTO::getName)
                .filter(name -> name != null)
                .anyMatch(name -> name.toLowerCase().startsWith("license")
                        || name.toLowerCase().startsWith("licence"));
    }

    private boolean hasDescription(RepoData repoData) {
        return StringUtils.hasText(repoData.getRepoDetails().getDescription());
    }

    private boolean hasTopics(RepoData repoData) {
        return repoData.getRepoDetails().isHasTopics();
    }
}
