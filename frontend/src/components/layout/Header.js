import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useOfflineMode } from '../../contexts/OfflineModeContext';
import styles from './Header.module.css';

const navItems = [
  { path: '/', label: 'Analyze' },
  { path: '/compare', label: 'Compare' },
  { path: '/trends', label: 'Trends' },
];

function Header() {
  const location = useLocation();
  const { isOffline, toggleOffline } = useOfflineMode();

  return (
    <header className={styles.header}>
      <div className={styles.logo}>
        <span className={styles.logoIcon}>🔍</span>
        <span className={styles.logoText}>GitHub Analyzer</span>
      </div>

      <nav className={styles.nav}>
        {navItems.map(({ path, label }) => {
          const active = location.pathname === path;
          return (
            <Link
              key={path}
              to={path}
              className={`${styles.navLink} ${active ? styles.navLinkActive : ''}`}
            >
              {label}
            </Link>
          );
        })}
      </nav>

      <div className={styles.offlineToggle}>
        <span className={styles.offlineLabel}>
          {isOffline ? '📴 Offline' : '🌐 Online'}
        </span>
        <button
          role="switch"
          aria-checked={isOffline}
          aria-label="Toggle offline mode"
          onClick={toggleOffline}
          className={styles.toggleBtn}
          style={{ backgroundColor: isOffline ? '#eab308' : '#374151' }}
        >
          <span
            className={styles.toggleThumb}
            style={{ left: isOffline ? '22px' : '2px' }}
          />
        </button>
      </div>
    </header>
  );
}

export default Header;
