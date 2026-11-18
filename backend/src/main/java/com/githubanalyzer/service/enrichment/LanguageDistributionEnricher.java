package com.githubanalyzer.service.enrichment;

import com.githubanalyzer.domain.LanguageDistribution;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class LanguageDistributionEnricher {

    public LanguageDistribution enrich(Map<String, Long> rawBytes) {
        if (rawBytes == null || rawBytes.isEmpty()) {
            return new LanguageDistribution(Collections.emptyMap(), Collections.emptyMap(), "Unknown");
        }

        long totalBytes = rawBytes.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Double> percentages = rawBytes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Math.round((double) e.getValue() / totalBytes * 1000.0) / 10.0,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        String primaryLanguage = rawBytes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        return new LanguageDistribution(rawBytes, percentages, primaryLanguage);
    }
}
