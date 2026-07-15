package com.githubanalyzer.domain;

import java.util.Collections;
import java.util.Map;

/**
 * Language distribution for a repository.
 *
 * <p>Raw byte counts are sourced from {@code GET /repos/{owner}/{repo}/languages}.
 * Percentages are computed by the {@code LanguageDistributionEnricher}.
 */
public final class LanguageDistribution {

    private final Map<String, Long> rawBytes;
    private final Map<String, Double> percentages;
    private final String primaryLanguage;

    public LanguageDistribution(Map<String, Long> rawBytes,
                                Map<String, Double> percentages,
                                String primaryLanguage) {
        this.rawBytes = Collections.unmodifiableMap(rawBytes);
        this.percentages = Collections.unmodifiableMap(percentages);
        this.primaryLanguage = primaryLanguage;
    }

    public Map<String, Long> getRawBytes() { return rawBytes; }
    public Map<String, Double> getPercentages() { return percentages; }
    public String getPrimaryLanguage() { return primaryLanguage; }
}
