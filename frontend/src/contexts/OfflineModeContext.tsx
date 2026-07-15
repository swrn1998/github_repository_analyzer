import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';

interface OfflineModeContextType {
  isOffline: boolean;
  setOffline: (value: boolean) => void;
  toggleOffline: () => void;
}

const OfflineModeContext = createContext<OfflineModeContextType | undefined>(undefined);

/**
 * Provides offline mode state to the entire application.
 *
 * Persists the offline preference to sessionStorage so it survives
 * page refreshes within the same browser tab.
 *
 * The httpClient reads from sessionStorage to inject the X-Offline-Mode header.
 */
export const OfflineModeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isOffline, setIsOffline] = useState<boolean>(() => {
    return sessionStorage.getItem('offlineMode') === 'true';
  });

  const setOffline = useCallback((value: boolean) => {
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
};

/**
 * Hook to consume offline mode state from any component.
 * Must be used within an OfflineModeProvider.
 */
export const useOfflineMode = (): OfflineModeContextType => {
  const context = useContext(OfflineModeContext);
  if (!context) {
    throw new Error('useOfflineMode must be used within an OfflineModeProvider');
  }
  return context;
};

export default OfflineModeContext;
