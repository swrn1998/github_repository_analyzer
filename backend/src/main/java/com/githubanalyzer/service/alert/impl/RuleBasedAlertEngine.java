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

// SOLID - Open/Closed: new alert types can be added by creating a new AlertRule bean
// without modifying this class at all
// Design pattern: variant of Chain of Responsibility where ALL matching rules fire (not just first)
@Service
public class RuleBasedAlertEngine implements AlertEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedAlertEngine.class);

    private final List<AlertRule> rules;

    // Spring automatically collects all @Component beans that implement AlertRule into this list
    // Adding a new rule = create a new class + annotate it with @Component. Done.
    public RuleBasedAlertEngine(List<AlertRule> rules) {
        this.rules = rules;
        log.info("AlertEngine loaded {} rules: {}", rules.size(),
                rules.stream().map(r -> r.getClass().getSimpleName()).collect(Collectors.joining(", ")));
    }

    @Override
    public List<Alert> evaluate(RepoData repoData) {
        // Each rule is evaluated independently - multiple alerts can fire for one repo
        return rules.stream()
                .filter(rule -> rule.applies(repoData))
                .map(rule -> rule.generateAlert(repoData))
                .collect(Collectors.toList());
    }
}
