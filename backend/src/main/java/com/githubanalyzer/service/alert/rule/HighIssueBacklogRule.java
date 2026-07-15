package com.githubanalyzer.service.alert.rule;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.domain.enums.AlertType;
import com.githubanalyzer.domain.enums.Severity;
import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;

/**
 * Alert rule: fires when the repository has more than 50 open issues.
 *
 * <p>A high open issue count can indicate:
 * <ul>
 *   <li>Insufficient maintainer bandwidth to triage/close issues</li>
 *   <li>Rapid growth of the user base creating more bug reports</li>
 *   <li>A policy of keeping issues open for discussion</li>
 * </ul>
 *
 * <p>The alert message surfaces this as a "maintenance concern" rather than
 * a definitive quality problem, acknowledging the ambiguity.
 */
@Component
public class HighIssueBacklogRule implements AlertRule {

    public static final int ISSUE_THRESHOLD = 50;
    private static final String MESSAGE = "High issue backlog - Maintenance concern";

    @Override
    public boolean applies(RepoData repoData) {
        if (repoData.getRepoDetails() == null) return false;
        return repoData.getRepoDetails().getOpenIssuesCount() > ISSUE_THRESHOLD;
    }

    @Override
    public Alert generateAlert(RepoData repoData) {
        return Alert.of(AlertType.HIGH_ISSUE_BACKLOG, MESSAGE, Severity.WARNING);
    }
}
