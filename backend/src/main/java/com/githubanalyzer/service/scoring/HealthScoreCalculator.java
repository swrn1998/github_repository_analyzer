package com.githubanalyzer.service.scoring;

import com.githubanalyzer.domain.HealthScore;
import com.githubanalyzer.dto.internal.RepoData;

/**
 * Contract for computing a repository health score from raw GitHub data.
 *
 * <p>Implementations aggregate scores from multiple {@code ScoreComponent}
 * strategies into a single composite score (0–100) and letter grade.
 */
public interface HealthScoreCalculator {

    /**
     * Calculates the health score for a repository.
     *
     * @param repoData aggregated raw data from GitHub
     * @return a {@link HealthScore} with total score, grade, and breakdown
     */
    HealthScore calculate(RepoData repoData);
}
