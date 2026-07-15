package com.githubanalyzer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.githubanalyzer.domain.enums.DataSource;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The full enriched response returned to the API consumer.
 *
 * <p>Contains raw stats, computed metrics, alerts, and HATEOAS links.
 * The {@code source} field tells the consumer how fresh the data is.
 *
 * <p>Use {@link Builder} for construction. Immutable after build.
 */
public final class AnalysisResponse {

    private final String owner;
    private final String repo;
    private final RepoStats stats;
    private final HealthScore healthScore;
    private final List<Alert> alerts;
    private final LanguageDistribution languages;
    private final ContributorActivity contributorActivity;
    private final DataSource source;
    private final Instant cachedAt;

    /** Optional human-readable note explaining why fallback data is shown (e.g. repo not found in offline mode). */
    private final String notice;

    @JsonProperty("_links")
    private final Map<String, Map<String, String>> links;

    private AnalysisResponse(Builder builder) {
        this.owner = builder.owner;
        this.repo = builder.repo;
        this.stats = builder.stats;
        this.healthScore = builder.healthScore;
        this.alerts = Collections.unmodifiableList(builder.alerts);
        this.languages = builder.languages;
        this.contributorActivity = builder.contributorActivity;
        this.source = builder.source;
        this.cachedAt = builder.cachedAt;
        this.notice = builder.notice;
        this.links = builder.links != null ? Collections.unmodifiableMap(builder.links) : Collections.emptyMap();
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public RepoStats getStats() { return stats; }
    public HealthScore getHealthScore() { return healthScore; }
    public List<Alert> getAlerts() { return alerts; }
    public LanguageDistribution getLanguages() { return languages; }
    public ContributorActivity getContributorActivity() { return contributorActivity; }
    public DataSource getSource() { return source; }
    public Instant getCachedAt() { return cachedAt; }
    public String getNotice() { return notice; }
    public Map<String, Map<String, String>> getLinks() { return links; }

    /**
     * Returns the canonical "owner/repo" identifier.
     */
    public String getFullName() {
        return owner + "/" + repo;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ── Builder ─────────────────────────────────────────────────────────────

    public static final class Builder {
        private String owner;
        private String repo;
        private RepoStats stats;
        private HealthScore healthScore;
        private List<Alert> alerts = Collections.emptyList();
        private LanguageDistribution languages;
        private ContributorActivity contributorActivity;
        private DataSource source = DataSource.LIVE;
        private Instant cachedAt;
        private String notice;
        private Map<String, Map<String, String>> links;

        public Builder owner(String owner) { this.owner = owner; return this; }
        public Builder repo(String repo) { this.repo = repo; return this; }
        public Builder stats(RepoStats stats) { this.stats = stats; return this; }
        public Builder healthScore(HealthScore healthScore) { this.healthScore = healthScore; return this; }
        public Builder alerts(List<Alert> alerts) { this.alerts = alerts; return this; }
        public Builder languages(LanguageDistribution languages) { this.languages = languages; return this; }
        public Builder contributorActivity(ContributorActivity activity) { this.contributorActivity = activity; return this; }
        public Builder source(DataSource source) { this.source = source; return this; }
        public Builder cachedAt(Instant cachedAt) { this.cachedAt = cachedAt; return this; }
        public Builder notice(String notice) { this.notice = notice; return this; }
        public Builder links(Map<String, Map<String, String>> links) { this.links = links; return this; }

        public AnalysisResponse build() {
            return new AnalysisResponse(this);
        }
    }
}
