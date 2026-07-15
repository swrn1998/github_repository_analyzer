package com.githubanalyzer.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

/**
 * REST response for the {@code GET /api/trends} endpoint.
 * Contains historical snapshots and derived summary statistics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TrendsResponse {

    private final String owner;
    private final String repo;
    private final List<TrendSnapshot> snapshots;
    private final TrendSummary summary;
    private final String message;  // For cases with insufficient data

    private TrendsResponse(Builder builder) {
        this.owner = builder.owner;
        this.repo = builder.repo;
        this.snapshots = Collections.unmodifiableList(builder.snapshots);
        this.summary = builder.summary;
        this.message = builder.message;
    }

    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public List<TrendSnapshot> getSnapshots() { return snapshots; }
    public TrendSummary getSummary() { return summary; }
    public String getMessage() { return message; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String owner;
        private String repo;
        private List<TrendSnapshot> snapshots = Collections.emptyList();
        private TrendSummary summary;
        private String message;

        public Builder owner(String v) { this.owner = v; return this; }
        public Builder repo(String v) { this.repo = v; return this; }
        public Builder snapshots(List<TrendSnapshot> v) { this.snapshots = v; return this; }
        public Builder summary(TrendSummary v) { this.summary = v; return this; }
        public Builder message(String v) { this.message = v; return this; }

        public TrendsResponse build() { return new TrendsResponse(this); }
    }
}
