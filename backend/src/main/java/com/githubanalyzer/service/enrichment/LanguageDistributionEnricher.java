package com.githubanalyzer.service.enrichment;

import com.githubanalyzer.domain.LanguageDistribution;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enriches raw language byte counts into a {@link LanguageDistribution}
 * with computed percentages and primary language identification.
 *
 * <p>GitHub returns raw byte counts per language. This enricher:
 * <ol>
 *   <li>Sums all byte counts</li>
 *   <li>Computes each language's percentage (rounded to 1 decimal)</li>
 *   <li>Identifies the primary language (highest byte count)</li>
 *   <li>Sorts by percentage descending</li>
 * </ol>
 */
@Component
public class LanguageDistributionEnricher {

    /**
     * Converts raw GitHub language bytes to a {@link LanguageDistribution}.
     *
     * @param rawBytes map of language name → byte count from GitHub API
     * @return enriched language distribution with percentages
     */
    public LanguageDistribution enrich(Map<String, Long> rawBytes) {
        if (rawBytes == null || rawBytes.isEmpty()) {
            return new LanguageDistribution(
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    "Unknown");
        }

        long totalBytes = rawBytes.values().stream().mapToLong(Long::longValue).sum();

        // Compute percentages, sorted descending by byte count
        Map<String, Double> percentages = rawBytes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Math.round((double) e.getValue() / totalBytes * 1000.0) / 10.0,
                        (a, b) -> a,
                        LinkedHashMap::new   // Preserve insertion order (sorted)
                ));

        // Primary language = highest byte count
        String primaryLanguage = rawBytes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        return new LanguageDistribution(rawBytes, percentages, primaryLanguage);
    }
}
