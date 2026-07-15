import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useOfflineMode } from '../../contexts/OfflineModeContext';

/**
 * Application header with navigation tabs and offline mode toggle.
 */
const Header: React.FC = () => {
  const location = useLocation();
  const { isOffline, toggleOffline } = useOfflineMode();

  const navItems = [
    { path: '/', label: 'Analyze' },
    { path: '/compare', label: 'Compare' },
    { path: '/trends', label: 'Trends' },
  ];

  return (
    <header style={{
      backgroundColor: '#1f2937',
      padding: '0 24px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      height: '56px',
      boxShadow: '0 2px 4px rgba(0,0,0,0.3)',
    }}>
      {/* Logo */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <span style={{ fontSize: '20px' }}>🔍</span>
        <span style={{ color: 'white', fontWeight: 700, fontSize: '16px' }}>
          GitHub Analyzer
        </span>
      </div>

      {/* Navigation */}
      <nav aria-label="Main navigation" style={{ display: 'flex', gap: '4px' }}>
        {navItems.map(({ path, label }) => (
          <Link
            key={path}
            to={path}
            aria-current={location.pathname === path ? 'page' : undefined}
            style={{
              color: location.pathname === path ? 'white' : '#9ca3af',
              textDecoration: 'none',
              padding: '6px 14px',
              borderRadius: '6px',
              fontSize: '14px',
              fontWeight: location.pathname === path ? 600 : 400,
              backgroundColor: location.pathname === path ? 'rgba(255,255,255,0.1)' : 'transparent',
            }}
          >
            {label}
          </Link>
        ))}
      </nav>

      {/* Offline mode toggle */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <span style={{ color: '#9ca3af', fontSize: '13px' }}>
          {isOffline ? '📴 Offline' : '🌐 Online'}
        </span>
        <button
          role="switch"
          aria-checked={isOffline}
          aria-label="Toggle offline mode"
          onClick={toggleOffline}
          style={{
            width: '44px',
            height: '24px',
            borderRadius: '12px',
            border: 'none',
            cursor: 'pointer',
            backgroundColor: isOffline ? '#eab308' : '#4b5563',
            position: 'relative',
            transition: 'background-color 0.2s',
          }}
        >
          <span style={{
            position: 'absolute',
            top: '2px',
            left: isOffline ? '22px' : '2px',
            width: '20px',
            height: '20px',
            borderRadius: '50%',
            backgroundColor: 'white',
            transition: 'left 0.2s',
          }} />
        </button>
      </div>
    </header>
  );
};

export default Header;
