package com.githubanalyzer.service.impl;

import com.githubanalyzer.domain.*;
import com.githubanalyzer.service.TrendsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// SOLID - Single Responsibility: manages historical trend snapshots only
// SOLID - Open/Closed: implements TrendsService interface - could swap to a DB-backed impl later
@Service
public class InMemoryTrendsService implements TrendsService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTrendsService.class);

    // ConcurrentHashMap keeps this thread-safe - multiple analyses can record snapshots concurrently
    private final Map<String, List<TrendSnapshot>> trendsStore = new ConcurrentHashMap<>();

    // 12-Factor III (Config): max snapshots per repo comes from environment variable
    // allows tuning memory usage without code changes
    @Value("${trends.max-snapshots-per-repo:30}")
    private int maxSnapshotsPerRepo;

    @Override
    public void recordSnapshot(String owner, String repo, AnalysisResponse analysis) {
        String key = buildKey(owner, repo);

        TrendSnapshot snapshot = TrendSnapshot.builder()
                .timestamp(Instant.now())
                .healthScore(analysis.getHealthScore().getScore())
                .grade(analysis.getHealthScore().getGrade())
                .stars(analysis.getStats().getStars())
                .forks(analysis.getStats().getForks())
                .openIssues(analysis.getStats().getOpenIssues())
                .totalContributors(analysis.getContributorActivity().getTotalContributors())
                .commits52Weeks(analysis.getContributorActivity().getTotalCommitsLast52Weeks())
                .primaryLanguage(analysis.getLanguages().getPrimaryLanguage())
                .build();

        // compute() is atomic - safe for concurrent updates on the same key
        trendsStore.compute(key, (k, existing) -> {
            List<TrendSnapshot> snapshots = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
            snapshots.add(snapshot);
            // Cap at maxSnapshotsPerRepo to prevent unbounded memory growth (12-Factor: stateless process)
            if (snapshots.size() > maxSnapshotsPerRepo) {
                snapshots = snapshots.stream()
                        .sorted(Comparator.comparing(TrendSnapshot::getTimestamp).reversed())
                        .limit(maxSnapshotsPerRepo)
                        .collect(Collectors.toList());
            }
            return snapshots;
        });
    }

    @Override
    public TrendsResponse getTrends(String owner, String repo) {
        String key = buildKey(owner, repo);
        List<TrendSnapshot> snapshots = trendsStore.getOrDefault(key, Collections.emptyList());

        if (snapshots.isEmpty()) {
            return TrendsResponse.builder()
                    .owner(owner).repo(repo)
                    .snapshots(Collections.emptyList())
                    .message("No historical data yet. Analyze this repository multiple times to build trend data.")
                    .build();
        }

        // Sort ascending by timestamp so the chart renders left-to-right chronologically
        List<TrendSnapshot> sorted = snapshots.stream()
                .sorted(Comparator.comparing(TrendSnapshot::getTimestamp))
                .collect(Collectors.toList());

        log.info("Retrieved {} trend snapshots for {}/{}", sorted.size(), owner, repo);

        return TrendsResponse.builder()
                .owner(owner).repo(repo)
                .snapshots(sorted)
                .summary(calculateSummary(sorted))
                .build();
    }

    @Override
    public int getTotalSnapshotCount() {
        return trendsStore.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public void clearAll() {
        int count = getTotalSnapshotCount();
        trendsStore.clear();
        log.info("Cleared all trend data: {} snapshots removed", count);
    }

    // key is lowercase "owner:repo" - consistent with how InMemoryCacheService keys work
    private String buildKey(String owner, String repo) {
        return owner.toLowerCase() + ":" + repo.toLowerCase();
    }

    private TrendSummary calculateSummary(List<TrendSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            // Not enough data points to compute a meaningful trend
            return TrendSummary.builder()
                    .healthScoreChange(0).healthScoreTrend("INSUFFICIENT_DATA")
                    .starsGrowth(0).starsGrowthRate("N/A")
                    .snapshotCount(snapshots.size()).daysTracked(0)
                    .build();
        }

        TrendSnapshot oldest = snapshots.get(0);
        TrendSnapshot newest = snapshots.get(snapshots.size() - 1);

        int healthScoreChange = newest.getHealthScore() - oldest.getHealthScore();
        long starsGrowth = newest.getStars() - oldest.getStars();
        long daysBetween = Duration.between(oldest.getTimestamp(), newest.getTimestamp()).toDays();

        return TrendSummary.builder()
                .healthScoreChange(healthScoreChange)
                .healthScoreTrend(determineHealthTrend(healthScoreChange))
                .starsGrowth(starsGrowth)
                .starsGrowthRate(calculateGrowthRate(oldest.getStars(), newest.getStars(), daysBetween))
                .snapshotCount(snapshots.size())
                .daysTracked((int) daysBetween)
                .build();
    }

    private String determineHealthTrend(int change) {
        // ±5 buffer avoids flipping between IMPROVING/DECLINING on tiny fluctuations
        if (change > 5) return "IMPROVING";
        if (change < -5) return "DECLINING";
        return "STABLE";
    }

    private String calculateGrowthRate(long oldValue, long newValue, long days) {
        if (oldValue == 0 || days == 0) return "N/A";
        double pct = ((double) (newValue - oldValue) / oldValue) * 100;
        return String.format("%s%.1f%% (%d days)", pct >= 0 ? "+" : "", pct, days);
    }
}
