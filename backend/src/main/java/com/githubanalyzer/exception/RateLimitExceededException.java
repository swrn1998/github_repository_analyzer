package com.githubanalyzer.exception;

/**
 * Thrown when GitHub returns HTTP 429 (rate limit exceeded) or when
 * the X-RateLimit-Remaining header shows 0 remaining requests.
 *
 * <p>When this occurs, the service falls back to stale cache or mock data
 * and logs a warning so operators know to rotate the API token.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long resetEpochSeconds;

    public RateLimitExceededException(long resetEpochSeconds) {
        super("GitHub API rate limit exceeded. Resets at epoch: " + resetEpochSeconds);
        this.resetEpochSeconds = resetEpochSeconds;
    }

    /**
     * Unix timestamp (seconds) when the rate limit window resets.
     * Sourced from the {@code X-RateLimit-Reset} response header.
     */
    public long getResetEpochSeconds() { return resetEpochSeconds; }
}
