package com.githubanalyzer.domain;

/**
 * Summary statistics derived from trend snapshots.
 * Provides high-level insights about repository trajectory.
 */
public final class TrendSummary {

    private final int healthScoreChange;
    private final String healthScoreTrend;  // "IMPROVING", "STABLE", "DECLINING"
    private final long starsGrowth;
    private final String starsGrowthRate;
    private final int snapshotCount;
    private final int daysTracked;

    private TrendSummary(Builder builder) {
        this.healthScoreChange = builder.healthScoreChange;
        this.healthScoreTrend = builder.healthScoreTrend;
        this.starsGrowth = builder.starsGrowth;
        this.starsGrowthRate = builder.starsGrowthRate;
        this.snapshotCount = builder.snapshotCount;
        this.daysTracked = builder.daysTracked;
    }

    public int getHealthScoreChange() { return healthScoreChange; }
    public String getHealthScoreTrend() { return healthScoreTrend; }
    public long getStarsGrowth() { return starsGrowth; }
    public String getStarsGrowthRate() { return starsGrowthRate; }
    public int getSnapshotCount() { return snapshotCount; }
    public int getDaysTracked() { return daysTracked; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int healthScoreChange;
        private String healthScoreTrend;
        private long starsGrowth;
        private String starsGrowthRate;
        private int snapshotCount;
        private int daysTracked;

        public Builder healthScoreChange(int v) { this.healthScoreChange = v; return this; }
        public Builder healthScoreTrend(String v) { this.healthScoreTrend = v; return this; }
        public Builder starsGrowth(long v) { this.starsGrowth = v; return this; }
        public Builder starsGrowthRate(String v) { this.starsGrowthRate = v; return this; }
        public Builder snapshotCount(int v) { this.snapshotCount = v; return this; }
        public Builder daysTracked(int v) { this.daysTracked = v; return this; }

        public TrendSummary build() { return new TrendSummary(this); }
    }
}
