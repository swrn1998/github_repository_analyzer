package com.githubanalyzer.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw DTO for the GitHub REST API repository contents response:
 * {@code GET /repos/{owner}/{repo}/contents}
 *
 * <p>Used by {@code NoTestInfrastructureRule} to detect the presence of
 * test directories or test files at the root level.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubContentDTO {

    private String name;
    private String type;     // "file" or "dir"
    private String path;

    @JsonProperty("download_url")
    private String downloadUrl;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    /**
     * Returns true if this entry is a directory (type == "dir").
     */
    public boolean isDirectory() {
        return "dir".equalsIgnoreCase(type);
    }

    /**
     * Returns true if this entry's name suggests test infrastructure.
     * Heuristic: matches common test folder/file naming conventions.
     */
    public boolean looksLikeTestEntry() {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.equals("test")
                || lower.equals("tests")
                || lower.equals("__tests__")
                || lower.equals("spec")
                || lower.equals("specs")
                || lower.startsWith("test_")
                || lower.endsWith("_test.py")
                || lower.endsWith(".test.ts")
                || lower.endsWith(".test.js")
                || lower.endsWith(".spec.ts")
                || lower.endsWith(".spec.js")
                || lower.endsWith("test.java")
                || lower.endsWith("tests.java");
    }
}
