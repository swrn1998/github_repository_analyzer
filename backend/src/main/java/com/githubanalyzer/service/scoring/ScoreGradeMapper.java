package com.githubanalyzer.service.scoring;

import org.springframework.stereotype.Component;

/**
 * Maps a numeric score (0–100) to a letter grade.
 *
 * <p>Grade scale:
 * <ul>
 *   <li>A: 90–100 (Excellent)</li>
 *   <li>B: 75–89  (Good)</li>
 *   <li>C: 60–74  (Fair)</li>
 *   <li>D: 40–59  (Poor)</li>
 *   <li>F: 0–39   (Critical)</li>
 * </ul>
 *
 * <p>Extracted as a separate component so the grade thresholds are
 * centralized and testable in isolation.
 */
@Component
public class ScoreGradeMapper {

    public String toGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 75) return "B";
        if (score >= 60) return "C";
        if (score >= 40) return "D";
        return "F";
    }

    /**
     * Returns a human-readable label for the grade.
     */
    public String toLabel(String grade) {
        return switch (grade) {
            case "A" -> "Excellent";
            case "B" -> "Good";
            case "C" -> "Fair";
            case "D" -> "Poor";
            case "F" -> "Critical";
            default -> "Unknown";
        };
    }
}
