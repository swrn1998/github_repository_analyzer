package com.githubanalyzer.exception;

/**
 * Thrown when the GitHub REST API returns an unexpected HTTP status code
 * or when the request fails due to network issues.
 *
 * <p>This is caught by the {@code GlobalExceptionHandler} and triggers
 * the cache/stale/mock fallback logic in {@code AnalyzerServiceImpl}.
 */
public class GitHubApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public GitHubApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    public int getStatusCode() { return statusCode; }
    public String getResponseBody() { return responseBody; }

    @Override
    public String toString() {
        return "GitHubApiException{statusCode=" + statusCode + ", message=" + getMessage() + "}";
    }
}
