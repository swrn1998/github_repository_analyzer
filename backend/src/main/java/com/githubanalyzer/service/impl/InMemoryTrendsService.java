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

/**
 * In-memory implementation of {@link TrendsService}.
 *
 * <p>Stores trend snapshots in a ConcurrentHashMap with automatic eviction
 * based on max snapshots per repository. Thread-safe for concurrent access.
 */
@Service
public class InMemoryTrendsService implements TrendsService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTrendsService.class);

    // key: "owner:repo", value: list of snapshots (sorted by timestamp)
    private final Map<String, List<TrendSnapshot>> trendsStore = new ConcurrentHashMap<>();

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

        trendsStore.compute(key, (k, existing) -> {
            List<TrendSnapshot> snapshots = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
            snapshots.add(snapshot);

            // Keep only the most recent snapshots
            if (snapshots.size() > maxSnapshotsPerRepo) {
                snapshots = snapshots.stream()
                        .sorted(Comparator.comparing(TrendSnapshot::getTimestamp).reversed())
                        .limit(maxSnapshotsPerRepo)
                        .collect(Collectors.toList());
            }

            log.debug("Recorded trend snapshot for {}/{}: {} total snapshots", owner, repo, snapshots.size());
            return snapshots;
        });
    }

    @Override
    public TrendsResponse getTrends(String owner, String repo) {
        String key = buildKey(owner, repo);
        List<TrendSnapshot> snapshots = trendsStore.getOrDefault(key, Collections.emptyList());

        if (snapshots.isEmpty()) {
            log.debug("No trend data found for {}/{}", owner, repo);
            return TrendsResponse.builder()
                    .owner(owner)
                    .repo(repo)
                    .snapshots(Collections.emptyList())
                    .message("No historical data yet. Analyze this repository multiple times to build trend data.")
                    .build();
        }

        // Sort by timestamp ascending for proper trend visualization
        List<TrendSnapshot> sortedSnapshots = snapshots.stream()
                .sorted(Comparator.comparing(TrendSnapshot::getTimestamp))
                .collect(Collectors.toList());

        TrendSummary summary = calculateSummary(sortedSnapshots);

        log.info("Retrieved {} snapshots for {}/{}", sortedSnapshots.size(), owner, repo);

        return TrendsResponse.builder()
                .owner(owner)
                .repo(repo)
                .snapshots(sortedSnapshots)
                .summary(summary)
                .build();
    }

    @Override
    public int getTotalSnapshotCount() {
        return trendsStore.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    @Override
    public void clearAll() {
        int count = getTotalSnapshotCount();
        trendsStore.clear();
        log.info("Cleared all trend data: {} snapshots removed", count);
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private String buildKey(String owner, String repo) {
        return owner.toLowerCase() + ":" + repo.toLowerCase();
    }

    private TrendSummary calculateSummary(List<TrendSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return TrendSummary.builder()
                    .healthScoreChange(0)
                    .healthScoreTrend("INSUFFICIENT_DATA")
                    .starsGrowth(0)
                    .starsGrowthRate("N/A")
                    .snapshotCount(snapshots.size())
                    .daysTracked(0)
                    .build();
        }

        TrendSnapshot oldest = snapshots.get(0);
        TrendSnapshot newest = snapshots.get(snapshots.size() - 1);

        int healthScoreChange = newest.getHealthScore() - oldest.getHealthScore();
        String healthScoreTrend = determineHealthTrend(healthScoreChange);

        long starsGrowth = newest.getStars() - oldest.getStars();
        long daysBetween = Duration.between(oldest.getTimestamp(), newest.getTimestamp()).toDays();
        String starsGrowthRate = calculateGrowthRate(oldest.getStars(), newest.getStars(), daysBetween);

        return TrendSummary.builder()
                .healthScoreChange(healthScoreChange)
                .healthScoreTrend(healthScoreTrend)
                .starsGrowth(starsGrowth)
                .starsGrowthRate(starsGrowthRate)
                .snapshotCount(snapshots.size())
                .daysTracked((int) daysBetween)
                .build();
    }

    private String determineHealthTrend(int change) {
        if (change > 5) return "IMPROVING";
        if (change < -5) return "DECLINING";
        return "STABLE";
    }

    private String calculateGrowthRate(long oldValue, long newValue, long days) {
        if (oldValue == 0 || days == 0) return "N/A";
        
        double percentChange = ((double) (newValue - oldValue) / oldValue) * 100;
        String sign = percentChange >= 0 ? "+" : "";
        
        return String.format("%s%.1f%% (%d days)", sign, percentChange, days);
    }
}
