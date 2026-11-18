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

// SOLID - Single Responsibility: aggregates four scoring dimensions into one final score
// SOLID - Open/Closed: new score dimensions can be added without changing this class
// Design pattern: Strategy - each ScoreComponent is an interchangeable scoring algorithm
@Service
public class WeightedHealthScoreCalculator implements HealthScoreCalculator {

    private static final Logger log = LoggerFactory.getLogger(WeightedHealthScoreCalculator.class);

    // SOLID - Dependency Inversion: depends on concrete components here (they are leaf classes)
    // but the calculator itself is accessed via HealthScoreCalculator interface by its callers
    private final CommitActivityScoreComponent commitActivityComponent;     // max 25 pts
    private final IssueRatioScoreComponent issueRatioComponent;             // max 25 pts
    private final CommunityEngagementScoreComponent communityComponent;     // max 25 pts
    private final DocumentationQualityScoreComponent documentationComponent;// max 25 pts
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
        // Each component scores independently (0-25), total is 0-100
        int commitScore    = commitActivityComponent.score(repoData);
        int issueScore     = issueRatioComponent.score(repoData);
        int communityScore = communityComponent.score(repoData);
        int docScore       = documentationComponent.score(repoData);

        ScoreBreakdown breakdown = new ScoreBreakdown(commitScore, issueScore, communityScore, docScore);
        int total = breakdown.getTotal();
        String grade = gradeMapper.toGrade(total); // maps 0-100 score to A/B/C/D/F

        log.debug("Health score for {}/{}: {} ({})", repoData.getOwner(), repoData.getRepo(), total, grade);
        return HealthScore.of(total, grade, breakdown);
    }
}
