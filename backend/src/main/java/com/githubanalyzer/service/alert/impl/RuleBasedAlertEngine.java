package com.githubanalyzer.service.alert.impl;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.dto.internal.RepoData;
import com.githubanalyzer.service.alert.AlertEngine;
import com.githubanalyzer.service.alert.rule.AlertRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluates all registered {@link AlertRule} implementations and
 * collects every alert that applies to the given repository data.
 *
 * <p>All rules are evaluated independently — multiple alerts can fire
 * for a single repository. Rules are injected by Spring as a list,
 * so adding a new {@code @Component AlertRule} automatically includes it.
 *
 * <p>Design: Chain of Responsibility variant where all applicable handlers
 * fire rather than the first match stopping the chain.
 */
@Service
public class RuleBasedAlertEngine implements AlertEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedAlertEngine.class);

    private final List<AlertRule> rules;

    /**
     * Spring injects all {@code AlertRule} beans into this list.
     * Order is determined by {@code @Order} annotations or Spring's default ordering.
     */
    public RuleBasedAlertEngine(List<AlertRule> rules) {
        this.rules = rules;
        log.info("AlertEngine initialized with {} rules: {}",
                rules.size(),
                rules.stream().map(r -> r.getClass().getSimpleName()).collect(Collectors.joining(", ")));
    }

    @Override
    public List<Alert> evaluate(RepoData repoData) {
        log.debug("Evaluating {} alert rules for {}/{}", rules.size(), repoData.getOwner(), repoData.getRepo());

        List<Alert> alerts = rules.stream()
                .filter(rule -> {
                    boolean applies = rule.applies(repoData);
                    log.debug("Rule {} applies: {}", rule.getClass().getSimpleName(), applies);
                    return applies;
                })
                .map(rule -> rule.generateAlert(repoData))
                .collect(Collectors.toList());

        log.debug("Alert evaluation complete: {} alert(s) raised", alerts.size());
        return alerts;
    }
}
