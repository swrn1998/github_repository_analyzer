package com.githubanalyzer.service;

import com.githubanalyzer.domain.AnalysisResponse;
import com.githubanalyzer.domain.TrendsResponse;

/**
 * Service for managing and retrieving historical trend data.
 *
 * <p>Snapshots are stored in-memory with a maximum limit per repository.
 * Old snapshots are automatically evicted to prevent unbounded growth.
 */
public interface TrendsService {

    /**
     * Records a snapshot from a successful analysis.
     * Called automatically after fetching fresh data from GitHub API.
     *
     * @param owner repository owner
     * @param repo repository name
     * @param analysis the complete analysis response to snapshot
     */
    void recordSnapshot(String owner, String repo, AnalysisResponse analysis);

    /**
     * Retrieves historical trends for a repository.
     *
     * @param owner repository owner
     * @param repo repository name
     * @return trends response with snapshots and summary, or empty if no data
     */
    TrendsResponse getTrends(String owner, String repo);

    /**
     * Returns the number of snapshots currently stored across all repositories.
     */
    int getTotalSnapshotCount();

    /**
     * Clears all trend data (useful for testing or manual cleanup).
     */
    void clearAll();
}
