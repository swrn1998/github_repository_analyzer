import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { OfflineModeProvider } from './contexts/OfflineModeContext';
import { AnalysisProvider } from './contexts/AnalysisContext';
import Header from './components/layout/Header';
import AnalyzePage from './pages/AnalyzePage';
import './App.css';

const ComparePage = React.lazy(() => import('./pages/ComparePage'));
const TrendsPage = React.lazy(() => import('./pages/TrendsPage'));

function App() {
  return (
    <OfflineModeProvider>
      <AnalysisProvider>
        <BrowserRouter>
          <div className="wrapper">
            <Header />
            <main>
              <React.Suspense fallback={
                <div className="suspenseFallback">Loading...</div>
              }>
                <Routes>
                  <Route path="/" element={<AnalyzePage />} />
                  <Route path="/compare" element={<ComparePage />} />
                  <Route path="/trends" element={<TrendsPage />} />
                  <Route path="*" element={
                    <div className="notFound">
                      <div className="notFoundCode">404</div>
                      <div className="notFoundText">Page not found</div>
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
}

export default App;
