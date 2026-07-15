package com.githubanalyzer.service.scoring.component;

import com.githubanalyzer.dto.internal.RepoData;

/**
 * Abstract base for {@link ScoreComponent} implementations.
 *
 * <p>Implements the Template Method pattern: {@link #score(RepoData)} defines
 * the skeleton of the algorithm (clamping, null-safety) and delegates the
 * actual computation to {@link #computeRawScore(RepoData)}.
 *
 * <p>Subclasses only need to implement {@link #computeRawScore(RepoData)} —
 * ceiling enforcement and defensive null handling are handled here.
 */
public abstract class BaseScoreComponent implements ScoreComponent {

    /**
     * Template method — calls {@link #computeRawScore} and enforces
     * the [0, maxScore] range.
     */
    @Override
    public final int score(RepoData repoData) {
        if (repoData == null || repoData.getRepoDetails() == null) {
            return 0;   // Null Object pattern: missing data scores zero
        }
        int raw = computeRawScore(repoData);
        return Math.max(0, Math.min(raw, getMaxScore()));
    }

    /**
     * Hook method — subclasses compute the raw (unclamped) score here.
     *
     * @param repoData guaranteed non-null with non-null repoDetails
     * @return raw score (may exceed max; clamping is applied by the template)
     */
    protected abstract int computeRawScore(RepoData repoData);
}
