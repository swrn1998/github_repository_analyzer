package com.githubanalyzer.service.alert.rule;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.dto.internal.RepoData;

/**
 * Contract for a single alert rule in the alert evaluation chain.
 *
 * <p>Each rule is responsible for:
 * <ol>
 *   <li>Deciding whether it applies to the given repository data</li>
 *   <li>Generating the specific alert if it does apply</li>
 * </ol>
 *
 * <p>Rules are evaluated independently — multiple rules can fire simultaneously.
 * This is different from the Chain of Responsibility pattern where only one handler
 * processes a request; here, all applicable rules produce an alert.
 *
 * <p>Adding a new alert = implement this interface + add to the list in
 * {@code RuleBasedAlertEngine}. No existing code changes required.
 */
public interface AlertRule {

    /**
     * Returns true if this rule's conditions are met for the given repository.
     *
     * @param repoData aggregated raw GitHub data
     * @return true if the rule should fire an alert
     */
    boolean applies(RepoData repoData);

    /**
     * Generates the alert for this rule.
     * Only called when {@link #applies(RepoData)} returns true.
     *
     * @param repoData aggregated raw GitHub data
     * @return the alert to include in the response
     */
    Alert generateAlert(RepoData repoData);
}
