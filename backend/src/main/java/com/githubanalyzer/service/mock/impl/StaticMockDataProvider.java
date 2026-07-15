package com.githubanalyzer.service.mock.impl;

import com.githubanalyzer.domain.*;
import com.githubanalyzer.domain.enums.AlertType;
import com.githubanalyzer.domain.enums.DataSource;
import com.githubanalyzer.domain.enums.Severity;
import com.githubanalyzer.service.mock.MockDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Provides hardcoded demo data for offline mode and API-unavailable fallback.
 *
 * <p>Implements the Null Object pattern — always returns a valid response,
 * never null, even for unknown repositories.
 *
 * <p>Pre-populated repos: facebook/react, microsoft/vscode, torvalds/linux.
 * All other repos fall back to a generic default mock.
 *
 * <p>The mock data is structured to exercise the UI meaningfully:
 * realistic score values, a mix of alerts, and representative language distributions.
 */
@Component
public class StaticMockDataProvider implements MockDataProvider {

    private static final Logger log = LoggerFactory.getLogger(StaticMockDataProvider.class);

    private final Map<String, AnalysisResponse> mockStore = new HashMap<>();

    public StaticMockDataProvider() {
        mockStore.put("facebook:react", buildReactMock());
        mockStore.put("microsoft:vscode", buildVsCodeMock());
        mockStore.put("torvalds:linux", buildLinuxMock());
        log.info("MockDataProvider loaded {} pre-built mock responses", mockStore.size());
    }

    @Override
    public AnalysisResponse getMockData(String owner, String repo) {
        String key = (owner + ":" + repo).toLowerCase();
        AnalysisResponse mock = mockStore.getOrDefault(key, buildDefaultMock(owner, repo));
        log.debug("Returning mock data for {}/{} (key={})", owner, repo, key);
        return mock;
    }

    // ── Mock builders ────────────────────────────────────────────────────────

    private AnalysisResponse buildReactMock() {
        return AnalysisResponse.builder()
                .owner("facebook").repo("react")
                .stats(RepoStats.builder()
                        .stars(220000).forks(45000).watchers(6700).openIssues(854)
                        .defaultBranch("main").description("The library for web and native user interfaces")
                        .license("MIT").hasWiki(true).hasTopics(true)
                        .createdAt(Instant.parse("2013-05-29T00:00:00Z"))
                        .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                        .pushedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                        .build())
                .healthScore(HealthScore.of(88, "B",
                        new ScoreBreakdown(25, 18, 25, 20)))
                .alerts(List.of(
                        Alert.of(AlertType.HIGH_ISSUE_BACKLOG, "High issue backlog - Maintenance concern", Severity.WARNING)
                ))
                .languages(new LanguageDistribution(
                        Map.of("JavaScript", 8200000L, "TypeScript", 1200000L, "CSS", 500000L),
                        Map.of("JavaScript", 82.5, "TypeScript", 12.3, "CSS", 5.2),
                        "JavaScript"))
                .contributorActivity(ContributorActivity.builder()
                        .totalContributors(1623)
                        .totalCommitsLast52Weeks(892)
                        .lastCommitDate(Instant.now().minus(2, ChronoUnit.HOURS))
                        .topContributors(List.of(
                                new ContributorSummary("gaearon", 2145, 48),
                                new ContributorSummary("sebmarkbage", 1876, 45),
                                new ContributorSummary("acdlite", 1234, 42)
                        ))
                        .build())
                .source(DataSource.MOCK)
                .cachedAt(Instant.now())
                .build();
    }

    private AnalysisResponse buildVsCodeMock() {
        return AnalysisResponse.builder()
                .owner("microsoft").repo("vscode")
                .stats(RepoStats.builder()
                        .stars(158000).forks(27800).watchers(3100).openIssues(8200)
                        .defaultBranch("main").description("Visual Studio Code")
                        .license("MIT").hasWiki(false).hasTopics(true)
                        .createdAt(Instant.parse("2015-09-03T00:00:00Z"))
                        .updatedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                        .pushedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                        .build())
                .healthScore(HealthScore.of(79, "B",
                        new ScoreBreakdown(25, 4, 25, 25)))
                .alerts(List.of(
                        Alert.of(AlertType.HIGH_ISSUE_BACKLOG, "High issue backlog - Maintenance concern", Severity.WARNING)
                ))
                .languages(new LanguageDistribution(
                        Map.of("TypeScript", 89400000L, "JavaScript", 6700000L, "CSS", 1900000L),
                        Map.of("TypeScript", 91.2, "JavaScript", 6.8, "CSS", 2.0),
                        "TypeScript"))
                .contributorActivity(ContributorActivity.builder()
                        .totalContributors(1947)
                        .totalCommitsLast52Weeks(2134)
                        .lastCommitDate(Instant.now().minus(1, ChronoUnit.HOURS))
                        .topContributors(List.of(
                                new ContributorSummary("bpasero", 3241, 52),
                                new ContributorSummary("joaomoreno", 2876, 51)
                        ))
                        .build())
                .source(DataSource.MOCK)
                .cachedAt(Instant.now())
                .build();
    }

    private AnalysisResponse buildLinuxMock() {
        return AnalysisResponse.builder()
                .owner("torvalds").repo("linux")
                .stats(RepoStats.builder()
                        .stars(175000).forks(52000).watchers(5600).openIssues(0)
                        .defaultBranch("master").description("Linux kernel source tree")
                        .license("GPL-2.0").hasWiki(false).hasTopics(false)
                        .createdAt(Instant.parse("2011-09-04T00:00:00Z"))
                        .updatedAt(Instant.now().minus(6, ChronoUnit.HOURS))
                        .pushedAt(Instant.now().minus(6, ChronoUnit.HOURS))
                        .build())
                .healthScore(HealthScore.of(91, "A",
                        new ScoreBreakdown(25, 25, 25, 16)))
                .alerts(Collections.emptyList())
                .languages(new LanguageDistribution(
                        Map.of("C", 990000000L, "Assembly", 20000000L, "Shell", 8000000L),
                        Map.of("C", 96.4, "Assembly", 1.9, "Shell", 0.8),
                        "C"))
                .contributorActivity(ContributorActivity.builder()
                        .totalContributors(20000)
                        .totalCommitsLast52Weeks(15000)
                        .lastCommitDate(Instant.now().minus(6, ChronoUnit.HOURS))
                        .topContributors(List.of(
                                new ContributorSummary("torvalds", 28000, 52),
                                new ContributorSummary("gregkh", 22000, 52)
                        ))
                        .build())
                .source(DataSource.MOCK)
                .cachedAt(Instant.now())
                .build();
    }

    private AnalysisResponse buildDefaultMock(String owner, String repo) {
        return AnalysisResponse.builder()
                .owner(owner).repo(repo)
                .stats(RepoStats.builder()
                        .stars(150).forks(30).watchers(12).openIssues(8)
                        .defaultBranch("main").description("Sample repository (demo mode)")
                        .license("MIT").hasWiki(false).hasTopics(false)
                        .createdAt(Instant.now().minus(365, ChronoUnit.DAYS))
                        .updatedAt(Instant.now().minus(7, ChronoUnit.DAYS))
                        .pushedAt(Instant.now().minus(7, ChronoUnit.DAYS))
                        .build())
                .healthScore(HealthScore.of(62, "C",
                        new ScoreBreakdown(20, 22, 8, 12)))
                .alerts(List.of(
                        Alert.of(AlertType.NO_TESTS, "Testing infrastructure not found", Severity.WARNING)
                ))
                .languages(new LanguageDistribution(
                        Map.of("Java", 45000L, "XML", 5000L),
                        Map.of("Java", 90.0, "XML", 10.0),
                        "Java"))
                .contributorActivity(ContributorActivity.builder()
                        .totalContributors(3)
                        .totalCommitsLast52Weeks(45)
                        .lastCommitDate(Instant.now().minus(7, ChronoUnit.DAYS))
                        .topContributors(List.of(
                                new ContributorSummary(owner, 45, 20)
                        ))
                        .build())
                .source(DataSource.MOCK)
                .cachedAt(Instant.now())
                .build();
    }
}
