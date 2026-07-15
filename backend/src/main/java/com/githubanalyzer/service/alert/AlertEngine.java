package com.githubanalyzer.service.alert;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.dto.internal.RepoData;

import java.util.List;

/**
 * Contract for evaluating alert rules against repository data.
 */
public interface AlertEngine {

    /**
     * Evaluates all registered rules and returns all applicable alerts.
     *
     * @param repoData aggregated raw GitHub data
     * @return list of alerts (may be empty if no conditions are met)
     */
    List<Alert> evaluate(RepoData repoData);
}
