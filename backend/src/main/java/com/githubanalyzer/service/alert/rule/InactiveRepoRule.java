package com.githubanalyzer.service.alert.rule;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.domain.enums.AlertType;
import com.githubanalyzer.domain.enums.Severity;
import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Alert rule: fires when no commits have been made in the last 90 days.
 *
 * <p>A repository with no recent commits may be abandoned or feature-complete.
 * The alert prompts the user to investigate before depending on the project.
 *
 * <p>If no commit date is available (e.g., API call failed), this rule does
 * NOT fire — we don't alert on missing data, only on confirmed inactivity.
 */
@Component
public class InactiveRepoRule implements AlertRule {

    private static final int INACTIVE_THRESHOLD_DAYS = 90;
    private static final String MESSAGE = "Repository appears inactive";

    @Override
    public boolean applies(RepoData repoData) {
        Instant lastCommit = repoData.getLastCommitDate();
        if (lastCommit == null) {
            return false;   // Can't confirm inactivity without data
        }
        long daysSince = ChronoUnit.DAYS.between(lastCommit, Instant.now());
        return daysSince > INACTIVE_THRESHOLD_DAYS;
    }

    @Override
    public Alert generateAlert(RepoData repoData) {
        return Alert.of(AlertType.INACTIVE_REPO, MESSAGE, Severity.WARNING);
    }
}
