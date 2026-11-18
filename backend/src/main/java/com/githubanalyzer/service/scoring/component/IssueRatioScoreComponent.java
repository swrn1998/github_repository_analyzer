package com.githubanalyzer.service.scoring.component;

import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;

@Component
public class IssueRatioScoreComponent extends BaseScoreComponent {

    private static final int MAX_SCORE = 25;

    @Override
    protected int computeRawScore(RepoData repoData) {
        long openIssues = repoData.getRepoDetails().getOpenIssuesCount();
        if (openIssues == 0)   return 25;
        if (openIssues <= 10)  return 22;
        if (openIssues <= 30)  return 18;
        if (openIssues <= 50)  return 13;
        if (openIssues <= 100) return 8;
        if (openIssues <= 200) return 4;
        return 0;
    }

    @Override
    public int getMaxScore() { return MAX_SCORE; }

    @Override
    public String getName() { return "issueRatio"; }
}
