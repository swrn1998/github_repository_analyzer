package com.githubanalyzer.service.mock;

import com.githubanalyzer.domain.AnalysisResponse;

/**
 * Contract for providing mock/demo analysis responses.
 *
 * <p>Used as the last resort in the degradation chain:
 * LIVE → CACHE → STALE → MOCK
 *
 * <p>Also used when the user explicitly enables offline mode with no
 * cached data available.
 */
public interface MockDataProvider {

    /**
     * Returns a mock {@link AnalysisResponse} for the given repository.
     *
     * <p>Implementations should first check for a repo-specific mock,
     * then fall back to a generic default mock.
     *
     * @param owner repository owner
     * @param repo  repository name
     * @return always returns a valid (non-null) response with {@code source = MOCK}
     */
    AnalysisResponse getMockData(String owner, String repo);
}
