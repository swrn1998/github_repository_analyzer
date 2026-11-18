package com.githubanalyzer.service;

import com.githubanalyzer.domain.AnalysisRequest;
import com.githubanalyzer.domain.AnalysisResponse;
import com.githubanalyzer.domain.ComparisonResponse;

public interface AnalyzerService {

    // Degradation order: fresh cache → live API → stale cache → error
    AnalysisResponse analyze(AnalysisRequest request);

    ComparisonResponse compare(AnalysisRequest requestA, AnalysisRequest requestB);
}
