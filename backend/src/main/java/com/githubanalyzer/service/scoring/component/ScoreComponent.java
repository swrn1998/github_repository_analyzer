package com.githubanalyzer.service.scoring.component;

import com.githubanalyzer.dto.internal.RepoData;

/**
 * Strategy interface for a single health score dimension.
 *
 * <p>Each implementation scores one aspect of repository health.
 * All components contribute up to {@link #getMaxScore()} points to the total.
 *
 * <p>Adding a new scoring dimension = implement this interface + register
 * the bean. Zero changes to {@code WeightedHealthScoreCalculator}.
 * This is the Open/Closed Principle in action.
 */
public interface ScoreComponent {

    /**
     * Computes the score for this dimension.
     *
     * @param repoData aggregated raw data from GitHub APIs
     * @return score in range [0, {@link #getMaxScore()}]
     */
    int score(RepoData repoData);

    /**
     * Returns the maximum possible points for this dimension.
     * All components should sum to 100 total.
     */
    int getMaxScore();

    /**
     * Human-readable name for this component, used in breakdown labels.
     */
    String getName();
}
