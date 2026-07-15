package com.githubanalyzer.service.scoring.component;

import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;

/**
 * Scores the repository based on its open issue count.
 *
 * <p><b>Scoring Logic (max 25 points):</b>
 * <ul>
 *   <li>Open issues = 0       → 25 (perfect maintenance)</li>
 *   <li>Open issues ≤ 10      → 22</li>
 *   <li>Open issues ≤ 30      → 18</li>
 *   <li>Open issues ≤ 50      → 13 (alert threshold)</li>
 *   <li>Open issues ≤ 100     → 8</li>
 *   <li>Open issues ≤ 200     → 4</li>
 *   <li>Open issues > 200     → 0</li>
 * </ul>
 *
 * <p>Note: Open issue count is a team culture metric, not just a bug count.
 * Active projects naturally have more issues. This is surfaced via the
 * trust boundary warning in the UI ("Score reflects issue count, not quality").
 *
 * <p>The threshold of 50 open issues also triggers the HIGH_ISSUE_BACKLOG alert.
 */
@Component
public class IssueRatioScoreComponent extends BaseScoreComponent {

    private static final int MAX_SCORE = 25;

    @Override
    protected int computeRawScore(RepoData repoData) {
        long openIssues = repoData.getRepoDetails().getOpenIssuesCount();

        if (openIssues == 0) return 25;
        if (openIssues <= 10) return 22;
        if (openIssues <= 30) return 18;
        if (openIssues <= 50) return 13;
        if (openIssues <= 100) return 8;
        if (openIssues <= 200) return 4;
        return 0;
    }

    @Override
    public int getMaxScore() {
        return MAX_SCORE;
    }

    @Override
    public String getName() {
        return "issueRatio";
    }
}
