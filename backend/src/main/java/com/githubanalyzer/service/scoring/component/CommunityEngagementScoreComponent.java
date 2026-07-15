package com.githubanalyzer.service.scoring.component;

import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;

/**
 * Scores the repository based on community engagement signals.
 *
 * <p><b>Scoring Logic (max 25 points):</b>
 * <p>Uses a logarithmic scale to prevent "winner takes all" — a repo with
 * 100,000 stars shouldn't score infinitely higher than one with 5,000.
 *
 * <p>Formula: {@code score = min(stars_points + forks_points + contributors_points, 25)}
 * <ul>
 *   <li>Stars:        log10(stars + 1) × 5      — max ~10 points at 100K stars</li>
 *   <li>Forks:        log10(forks + 1) × 5      — max ~10 points at 100K forks</li>
 *   <li>Contributors: log10(contributors + 1) × 3 — max ~5 points</li>
 * </ul>
 *
 * <p><b>Trust Boundary:</b> Stars can be gamed. This metric is an engagement
 * signal, not a quality guarantee. The UI surfaces this caveat.
 */
@Component
public class CommunityEngagementScoreComponent extends BaseScoreComponent {

    private static final int MAX_SCORE = 25;

    private static final double STARS_MULTIPLIER = 5.0;
    private static final double FORKS_MULTIPLIER = 5.0;
    private static final double CONTRIBUTORS_MULTIPLIER = 3.0;

    @Override
    protected int computeRawScore(RepoData repoData) {
        long stars = repoData.getRepoDetails().getStargazersCount();
        long forks = repoData.getRepoDetails().getForksCount();
        int contributors = repoData.getContributors().size();

        double starsScore = Math.log10(stars + 1) * STARS_MULTIPLIER;
        double forksScore = Math.log10(forks + 1) * FORKS_MULTIPLIER;
        double contributorScore = Math.log10(contributors + 1) * CONTRIBUTORS_MULTIPLIER;

        return (int) Math.round(starsScore + forksScore + contributorScore);
    }

    @Override
    public int getMaxScore() {
        return MAX_SCORE;
    }

    @Override
    public String getName() {
        return "community";
    }
}
