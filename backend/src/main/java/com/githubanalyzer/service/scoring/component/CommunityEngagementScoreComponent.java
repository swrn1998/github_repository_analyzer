package com.githubanalyzer.service.scoring.component;

import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;

@Component
public class CommunityEngagementScoreComponent extends BaseScoreComponent {

    private static final int MAX_SCORE = 25;

    // Logarithmic scale to prevent large repos from dominating unfairly
    @Override
    protected int computeRawScore(RepoData repoData) {
        long stars = repoData.getRepoDetails().getStargazersCount();
        long forks = repoData.getRepoDetails().getForksCount();
        int contributors = repoData.getContributors().size();

        double score = (Math.log10(stars + 1) * 5.0)
                     + (Math.log10(forks + 1) * 5.0)
                     + (Math.log10(contributors + 1) * 3.0);
        return (int) Math.round(score);
    }

    @Override
    public int getMaxScore() { return MAX_SCORE; }

    @Override
    public String getName() { return "community"; }
}
