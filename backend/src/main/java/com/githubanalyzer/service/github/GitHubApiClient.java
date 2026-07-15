package com.githubanalyzer.service.github;

import com.githubanalyzer.dto.github.*;

import java.util.List;
import java.util.Map;

/**
 * Contract for fetching raw data from the GitHub REST API.
 *
 * <p>All methods return raw DTOs. Enrichment (scoring, alerts, etc.)
 * happens in the service layer, not here.
 *
 * <p>Implementations must handle:
 * <ul>
 *   <li>Authentication via token header</li>
 *   <li>HTTP error mapping to domain exceptions</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>202 Accepted handling for stats endpoints</li>
 * </ul>
 */
public interface GitHubApiClient {

    /**
     * Fetches core repository metadata.
     *
     * @param owner repository owner (username or org)
     * @param repo  repository name
     * @return raw repo DTO
     * @throws com.githubanalyzer.exception.RepoNotFoundException   if GitHub returns 404
     * @throws com.githubanalyzer.exception.GitHubApiException      for other HTTP errors
     * @throws com.githubanalyzer.exception.RateLimitExceededException if rate limited
     */
    GitHubRepoDTO fetchRepoDetails(String owner, String repo);

    /**
     * Fetches contributor statistics for the last 52 weeks.
     *
     * <p>GitHub may return HTTP 202 if stats are being computed.
     * Implementations should retry up to 3 times with a short delay.
     *
     * @return list of contributor stats (may be empty if computing)
     */
    List<GitHubContributorDTO> fetchContributors(String owner, String repo);

    /**
     * Fetches language byte counts.
     *
     * @return map of language name → byte count
     */
    Map<String, Long> fetchLanguages(String owner, String repo);

    /**
     * Fetches the most recent commits (limited to {@code perPage} items).
     *
     * @param perPage number of commits to return (1–100)
     */
    List<GitHubCommitDTO> fetchRecentCommits(String owner, String repo, int perPage);

    /**
     * Fetches the root directory contents of the repository.
     * Used to detect the presence of test infrastructure.
     */
    List<GitHubContentDTO> fetchRootContents(String owner, String repo);
}
