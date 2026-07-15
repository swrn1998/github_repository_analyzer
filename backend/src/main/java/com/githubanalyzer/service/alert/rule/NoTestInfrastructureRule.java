package com.githubanalyzer.service.alert.rule;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.domain.enums.AlertType;
import com.githubanalyzer.domain.enums.Severity;
import com.githubanalyzer.dto.internal.RepoData;
import org.springframework.stereotype.Component;

/**
 * Alert rule: fires when no recognizable test infrastructure is detected.
 *
 * <p>Detection is heuristic — it looks for common test directory names and
 * file suffixes in the repository root contents:
 * <ul>
 *   <li>Directories: {@code test}, {@code tests}, {@code __tests__}, {@code spec}, {@code specs}</li>
 *   <li>File patterns: {@code *.test.ts}, {@code *.spec.js}, {@code *Test.java}, etc.</li>
 * </ul>
 *
 * <p><b>Trust Boundary:</b> This detection is NOT exhaustive. A repo may have
 * tests in subdirectories not visible in root contents, or use a non-standard
 * naming convention. The alert message reflects this uncertainty.
 *
 * <p>If root contents could not be fetched (empty list), the rule does NOT
 * fire — absence of data is not absence of tests.
 */
@Component
public class NoTestInfrastructureRule implements AlertRule {

    private static final String MESSAGE = "Testing infrastructure not found";

    @Override
    public boolean applies(RepoData repoData) {
        // If we couldn't fetch root contents, don't alert — we simply don't know
        if (repoData.getRootContents().isEmpty()) {
            return false;
        }
        return !repoData.hasTestFiles();
    }

    @Override
    public Alert generateAlert(RepoData repoData) {
        return Alert.of(AlertType.NO_TESTS, MESSAGE, Severity.WARNING);
    }
}
