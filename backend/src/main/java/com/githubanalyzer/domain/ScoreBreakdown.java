package com.githubanalyzer.domain;

/**
 * Breakdown of the health score by individual scoring dimension.
 *
 * <p>Each component contributes a maximum of 25 points to the total score of 100.
 * This breakdown is surfaced in the UI as a radar/bar chart.
 */
public final class ScoreBreakdown {

    private final int commitActivityScore;
    private final int issueRatioScore;
    private final int communityScore;
    private final int documentationScore;

    public ScoreBreakdown(int commitActivityScore, int issueRatioScore,
                          int communityScore, int documentationScore) {
        this.commitActivityScore = clamp(commitActivityScore, 0, 25);
        this.issueRatioScore = clamp(issueRatioScore, 0, 25);
        this.communityScore = clamp(communityScore, 0, 25);
        this.documentationScore = clamp(documentationScore, 0, 25);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Computes the total score as the sum of all components.
     * Maximum possible: 100.
     */
    public int getTotal() {
        return commitActivityScore + issueRatioScore + communityScore + documentationScore;
    }

    public int getCommitActivityScore() { return commitActivityScore; }
    public int getIssueRatioScore() { return issueRatioScore; }
    public int getCommunityScore() { return communityScore; }
    public int getDocumentationScore() { return documentationScore; }

    @Override
    public String toString() {
        return "ScoreBreakdown{commit=" + commitActivityScore
                + ", issue=" + issueRatioScore
                + ", community=" + communityScore
                + ", docs=" + documentationScore
                + ", total=" + getTotal() + "}";
    }
}
