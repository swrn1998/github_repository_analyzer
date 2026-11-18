package com.githubanalyzer.service.github;

import com.githubanalyzer.dto.github.*;

import java.util.List;
import java.util.Map;

public interface GitHubApiClient {

    // Throws RepoNotFoundException on 404, RateLimitExceededException on 429
    GitHubRepoDTO fetchRepoDetails(String owner, String repo);

    // Returns empty list on failure — non-fatal for the analysis pipeline
    List<GitHubContributorDTO> fetchContributors(String owner, String repo);

    Map<String, Long> fetchLanguages(String owner, String repo);

    List<GitHubCommitDTO> fetchRecentCommits(String owner, String repo, int perPage);

    List<GitHubContentDTO> fetchRootContents(String owner, String repo);
}
