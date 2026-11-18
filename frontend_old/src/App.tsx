import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { OfflineModeProvider } from './contexts/OfflineModeContext';
import { AnalysisProvider } from './contexts/AnalysisContext';
import Header from './components/layout/Header';
import AnalyzePage from './pages/AnalyzePage';

// Lazy-loaded pages for code splitting
const ComparePage = React.lazy(() => import('./pages/ComparePage'));
const TrendsPage = React.lazy(() => import('./pages/TrendsPage'));

/**
 * Application root — sets up:
 * 1. Context providers (offline mode, analysis state)
 * 2. React Router
 * 3. Lazy-loaded page routes
 */
const App: React.FC = () => {
  return (
    <OfflineModeProvider>
      <AnalysisProvider>
        <BrowserRouter>
          <div style={{ minHeight: '100vh', backgroundColor: '#f9fafb', fontFamily: 'system-ui, -apple-system, sans-serif' }}>
            <Header />
            <main>
              <React.Suspense fallback={
                <div style={{ textAlign: 'center', padding: '60px', color: '#6b7280' }}>
                  Loading...
                </div>
              }>
                <Routes>
                  <Route path="/" element={<AnalyzePage />} />
                  <Route path="/compare" element={<ComparePage />} />
                  <Route path="/trends" element={<TrendsPage />} />
                  <Route path="*" element={
                    <div style={{ textAlign: 'center', padding: '60px', color: '#6b7280' }}>
                      <div style={{ fontSize: '48px' }}>404</div>
                      <div>Page not found</div>
                    </div>
                  } />
                </Routes>
              </React.Suspense>
            </main>
          </div>
        </BrowserRouter>
      </AnalysisProvider>
    </OfflineModeProvider>
  );
};

export default App;
