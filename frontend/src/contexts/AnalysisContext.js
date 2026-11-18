import React, { createContext, useCallback, useContext, useState } from 'react';
import { analyzeRepo } from '../services/analyzerApi';

const AnalysisContext = createContext(null);

export function AnalysisProvider({ children }) {
  const [result, setResult] = useState(null);
  const [status, setStatus] = useState('idle');
  const [error, setError] = useState(null);

  const analyze = useCallback(async (owner, repo) => {
    setStatus('loading');
    setError(null);
    try {
      const data = await analyzeRepo(owner, repo);
      setResult(data);
      setStatus('success');
    } catch (err) {
      setError(err);
      setStatus('error');
    }
  }, []);

  const clearResult = useCallback(() => {
    setResult(null);
    setStatus('idle');
    setError(null);
  }, []);

  return (
    <AnalysisContext.Provider value={{ result, status, error, analyze, clearResult }}>
      {children}
    </AnalysisContext.Provider>
  );
}

export function useAnalysis() {
  const ctx = useContext(AnalysisContext);
  if (!ctx) throw new Error('useAnalysis must be inside AnalysisProvider');
  return ctx;
}
