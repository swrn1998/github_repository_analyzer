package com.githubanalyzer.service.scoring.component;

import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class CommitActivityScoreComponent extends BaseScoreComponent {

    private static final int MAX_SCORE = 25;

    @Override
    protected int computeRawScore(RepoData repoData) {
        Instant lastCommit = repoData.getLastCommitDate();
        if (lastCommit == null) return 10; // no data — can't confirm active or inactive

        long daysSince = ChronoUnit.DAYS.between(lastCommit, Instant.now());
        if (daysSince <= 7)  return 25;
        if (daysSince <= 30) return 20;
        if (daysSince <= 60) return 13;
        if (daysSince <= 90) return 5;
        return 0;
    }

    @Override
    public int getMaxScore() { return MAX_SCORE; }

    @Override
    public String getName() { return "commitActivity"; }
}
