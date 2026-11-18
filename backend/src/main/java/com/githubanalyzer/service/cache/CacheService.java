package com.githubanalyzer.service.cache;

import com.githubanalyzer.domain.AnalysisResponse;

import java.util.Optional;

public interface CacheService {

    // Returns fresh entry only (within TTL)
    Optional<AnalysisResponse> get(String key);

    // Returns expired entry as fallback when live API fails
    Optional<AnalysisResponse> getStale(String key);

    void put(String key, AnalysisResponse value);

    void evict(String key);

    void clear();

    CacheStats getStats();
}
