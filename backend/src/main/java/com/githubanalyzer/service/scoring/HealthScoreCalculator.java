package com.githubanalyzer.service.scoring;

import com.githubanalyzer.domain.HealthScore;
import com.githubanalyzer.dto.internal.RepoData;

public interface HealthScoreCalculator {

    HealthScore calculate(RepoData repoData);
}
