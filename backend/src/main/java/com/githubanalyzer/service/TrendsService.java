package com.githubanalyzer.service;

import com.githubanalyzer.domain.AnalysisResponse;
import com.githubanalyzer.domain.TrendsResponse;

public interface TrendsService {

    void recordSnapshot(String owner, String repo, AnalysisResponse analysis);

    TrendsResponse getTrends(String owner, String repo);

    int getTotalSnapshotCount();

    void clearAll();
}
