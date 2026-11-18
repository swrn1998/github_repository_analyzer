import React, { createContext, useCallback, useContext, useState } from 'react';

const OfflineModeContext = createContext(null);

export function OfflineModeProvider({ children }) {
  const [isOffline, setIsOffline] = useState(
    () => sessionStorage.getItem('offlineMode') === 'true'
  );

  const setOffline = useCallback((value) => {
    setIsOffline(value);
    sessionStorage.setItem('offlineMode', String(value));
  }, []);

  const toggleOffline = useCallback(() => {
    setOffline(!isOffline);
  }, [isOffline, setOffline]);

  return (
    <OfflineModeContext.Provider value={{ isOffline, setOffline, toggleOffline }}>
      {children}
    </OfflineModeContext.Provider>
  );
}

export function useOfflineMode() {
  const ctx = useContext(OfflineModeContext);
  if (!ctx) throw new Error('useOfflineMode must be inside OfflineModeProvider');
  return ctx;
}
