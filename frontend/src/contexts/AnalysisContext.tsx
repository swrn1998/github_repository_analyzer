import React, { createContext, useCallback, useContext, useState } from 'react';
import { AnalysisResponse, ApiError, RequestStatus } from '../types/analysis';
import { analyzeRepo } from '../services/analyzerApi';

interface AnalysisContextType {
  result: AnalysisResponse | null;
  status: RequestStatus;
  error: ApiError | null;
  analyze: (owner: string, repo: string) => Promise<void>;
  clearResult: () => void;
}

const AnalysisContext = createContext<AnalysisContextType | undefined>(undefined);

/**
 * Provides the current analysis result and the analyze action to the component tree.
 *
 * Manages loading/error states so all consuming components get consistent state.
 */
export const AnalysisProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [result, setResult] = useState<AnalysisResponse | null>(null);
  const [status, setStatus] = useState<RequestStatus>('idle');
  const [error, setError] = useState<ApiError | null>(null);

  const analyze = useCallback(async (owner: string, repo: string) => {
    setStatus('loading');
    setError(null);

    try {
      const data = await analyzeRepo(owner, repo);
      setResult(data);
      setStatus('success');
    } catch (err) {
      setError(err as ApiError);
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
};

export const useAnalysis = (): AnalysisContextType => {
  const context = useContext(AnalysisContext);
  if (!context) {
    throw new Error('useAnalysis must be used within an AnalysisProvider');
  }
  return context;
};

export default AnalysisContext;
