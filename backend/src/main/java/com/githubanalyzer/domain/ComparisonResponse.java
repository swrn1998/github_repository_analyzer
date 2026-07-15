package com.githubanalyzer.domain;

/**
 * Response for a side-by-side repository comparison.
 *
 * <p>Contains both full analysis responses and a computed winner summary.
 */
public final class ComparisonResponse {

    private final AnalysisResponse repoA;
    private final AnalysisResponse repoB;
    private final String winner;          // "owner/repo" of the healthier repository
    private final int healthScoreDelta;   // repoA.score - repoB.score
    private final String summary;

    public ComparisonResponse(AnalysisResponse repoA, AnalysisResponse repoB) {
        this.repoA = repoA;
        this.repoB = repoB;

        int scoreA = repoA.getHealthScore() != null ? repoA.getHealthScore().getScore() : 0;
        int scoreB = repoB.getHealthScore() != null ? repoB.getHealthScore().getScore() : 0;
        this.healthScoreDelta = scoreA - scoreB;

        if (scoreA > scoreB) {
            this.winner = repoA.getFullName();
            this.summary = repoA.getFullName() + " is healthier by " + healthScoreDelta + " points";
        } else if (scoreB > scoreA) {
            this.winner = repoB.getFullName();
            this.summary = repoB.getFullName() + " is healthier by " + Math.abs(healthScoreDelta) + " points";
        } else {
            this.winner = "tie";
            this.summary = "Both repositories have equal health scores";
        }
    }

    public AnalysisResponse getRepoA() { return repoA; }
    public AnalysisResponse getRepoB() { return repoB; }
    public String getWinner() { return winner; }
    public int getHealthScoreDelta() { return healthScoreDelta; }
    public String getSummary() { return summary; }
}
