package com.githubanalyzer.service.scoring.impl;

import com.githubanalyzer.domain.HealthScore;
import com.githubanalyzer.domain.ScoreBreakdown;
import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.scoring.HealthScoreCalculator;
import com.githubanalyzer.service.scoring.ScoreGradeMapper;
import com.githubanalyzer.service.scoring.component.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Computes the health score by aggregating the four {@link ScoreComponent} strategies.
 *
 * <p>This is the Facade over the individual scoring strategies. Each component
 * scores one dimension (max 25 pts each), and the sum becomes the total (max 100).
 *
 * <p>The four components are injected by Spring — this class has no knowledge
 * of their internal implementation. New components can be added without
 * modifying this class (Open/Closed Principle).
 *
 * <p>Component order for breakdown:
 * <ol>
 *   <li>Commit Activity       (0–25)</li>
 *   <li>Issue Ratio           (0–25)</li>
 *   <li>Community Engagement  (0–25)</li>
 *   <li>Documentation Quality (0–25)</li>
 * </ol>
 */
@Service
public class WeightedHealthScoreCalculator implements HealthScoreCalculator {

    private static final Logger log = LoggerFactory.getLogger(WeightedHealthScoreCalculator.class);

    private final CommitActivityScoreComponent commitActivityComponent;
    private final IssueRatioScoreComponent issueRatioComponent;
    private final CommunityEngagementScoreComponent communityComponent;
    private final DocumentationQualityScoreComponent documentationComponent;
    private final ScoreGradeMapper gradeMapper;

    public WeightedHealthScoreCalculator(
            CommitActivityScoreComponent commitActivityComponent,
            IssueRatioScoreComponent issueRatioComponent,
            CommunityEngagementScoreComponent communityComponent,
            DocumentationQualityScoreComponent documentationComponent,
            ScoreGradeMapper gradeMapper) {
        this.commitActivityComponent = commitActivityComponent;
        this.issueRatioComponent = issueRatioComponent;
        this.communityComponent = communityComponent;
        this.documentationComponent = documentationComponent;
        this.gradeMapper = gradeMapper;
    }

    @Override
    public HealthScore calculate(RepoData repoData) {
        log.debug("Calculating health score for {}/{}", repoData.getOwner(), repoData.getRepo());

        int commitScore   = commitActivityComponent.score(repoData);
        int issueScore    = issueRatioComponent.score(repoData);
        int communityScore = communityComponent.score(repoData);
        int docScore      = documentationComponent.score(repoData);

        ScoreBreakdown breakdown = new ScoreBreakdown(
                commitScore, issueScore, communityScore, docScore);

        int totalScore = breakdown.getTotal();
        String grade = gradeMapper.toGrade(totalScore);

        log.debug("Health score calculated: {} ({}) — breakdown: {}", totalScore, grade, breakdown);

        return HealthScore.of(totalScore, grade, breakdown);
    }
}
