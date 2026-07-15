package com.githubanalyzer.exception;

/**
 * Thrown when GitHub returns HTTP 404 for a repository lookup.
 *
 * <p>This is mapped to a 404 response by the {@code GlobalExceptionHandler}.
 * Unlike a generic API failure, a 404 means the repo doesn't exist —
 * there's no point falling back to cache or mock data.
 */
public class RepoNotFoundException extends RuntimeException {

    private final String owner;
    private final String repo;

    public RepoNotFoundException(String owner, String repo) {
        super("Repository not found: " + owner + "/" + repo);
        this.owner = owner;
        this.repo = repo;
    }

    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
}
