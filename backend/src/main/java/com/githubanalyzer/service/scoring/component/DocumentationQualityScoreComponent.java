package com.githubanalyzer.service.scoring.component;

import com.githubanalyzer.dto.github.GitHubContentDTO;
import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DocumentationQualityScoreComponent extends BaseScoreComponent {

    private static final int MAX_SCORE = 25;

    // README +7, LICENSE +7, description +6, topics +5
    @Override
    protected int computeRawScore(RepoData repoData) {
        int score = 0;
        if (hasReadme(repoData))       score += 7;
        if (hasLicense(repoData))      score += 7;
        if (hasDescription(repoData))  score += 6;
        if (hasTopics(repoData))       score += 5;
        return score;
    }

    @Override
    public int getMaxScore() { return MAX_SCORE; }

    @Override
    public String getName() { return "documentation"; }

    private boolean hasReadme(RepoData repoData) {
        return repoData.getRootContents().stream()
                .map(GitHubContentDTO::getName)
                .filter(name -> name != null)
                .anyMatch(name -> name.toLowerCase().startsWith("readme"));
    }

    private boolean hasLicense(RepoData repoData) {
        if (StringUtils.hasText(repoData.getRepoDetails().getLicenseName())) return true;
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
