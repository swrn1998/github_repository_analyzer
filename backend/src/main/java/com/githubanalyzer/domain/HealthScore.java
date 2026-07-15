package com.githubanalyzer.domain;

import java.util.Objects;

/**
 * Computed health score for a repository.
 *
 * <p>Score range: 0–100. Grade: A (90+), B (75+), C (60+), D (40+), F (<40).
 *
 * <p>Calculated by {@code WeightedHealthScoreCalculator} using four
 * {@code ScoreComponent} strategy implementations.
 */
public final class HealthScore {

    private final int score;
    private final String grade;
    private final ScoreBreakdown breakdown;

    private HealthScore(int score, String grade, ScoreBreakdown breakdown) {
        this.score = score;
        this.grade = Objects.requireNonNull(grade, "grade must not be null");
        this.breakdown = Objects.requireNonNull(breakdown, "breakdown must not be null");
    }

    /**
     * Factory method.
     *
     * @param score     total score 0–100
     * @param grade     letter grade string ("A", "B", "C", "D", "F")
     * @param breakdown per-dimension scores
     */
    public static HealthScore of(int score, String grade, ScoreBreakdown breakdown) {
        return new HealthScore(score, grade, breakdown);
    }

    public int getScore() { return score; }
    public String getGrade() { return grade; }
    public ScoreBreakdown getBreakdown() { return breakdown; }

    @Override
    public String toString() {
        return "HealthScore{score=" + score + ", grade='" + grade + "', breakdown=" + breakdown + "}";
    }
}
