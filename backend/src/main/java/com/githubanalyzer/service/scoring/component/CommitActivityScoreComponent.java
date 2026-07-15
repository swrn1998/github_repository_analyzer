package com.githubanalyzer.service.scoring.component;

import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scores the repository based on recent commit activity.
 *
 * <p><b>Scoring Logic (max 25 points):</b>
 * <ul>
 *   <li>Commit within last 7 days  → 25 (full score)</li>
 *   <li>Commit within last 30 days → 20</li>
 *   <li>Commit within last 60 days → 13</li>
 *   <li>Commit within last 90 days → 5</li>
 *   <li>No commit in 90+ days      → 0 (also triggers INACTIVE_REPO alert)</li>
 *   <li>No commit data available   → 10 (partial — data missing, not confirmed inactive)</li>
 * </ul>
 *
 * <p>This dimension directly drives the "Repository appears inactive" alert
 * in {@code InactiveRepoRule} — the alert and the score penalty are independent
 * but based on the same data.
 */
@Component
public class CommitActivityScoreComponent extends BaseScoreComponent {

    private static final int MAX_SCORE = 25;

    // Thresholds in days
    private static final int VERY_ACTIVE_DAYS = 7;
    private static final int ACTIVE_DAYS = 30;
    private static final int SLOW_DAYS = 60;
    private static final int INACTIVE_THRESHOLD_DAYS = 90;

    @Override
    protected int computeRawScore(RepoData repoData) {
        Instant lastCommit = repoData.getLastCommitDate();

        if (lastCommit == null) {
            // No commit data — we can't confirm activity, but also can't confirm inactivity
            return 10;
        }

        long daysSince = ChronoUnit.DAYS.between(lastCommit, Instant.now());

        if (daysSince <= VERY_ACTIVE_DAYS) return 25;
        if (daysSince <= ACTIVE_DAYS) return 20;
        if (daysSince <= SLOW_DAYS) return 13;
        if (daysSince <= INACTIVE_THRESHOLD_DAYS) return 5;
        return 0;   // Inactive — 90+ days without a commit
    }

    @Override
    public int getMaxScore() {
        return MAX_SCORE;
    }

    @Override
    public String getName() {
        return "commitActivity";
    }
}
