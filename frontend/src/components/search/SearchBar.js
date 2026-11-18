import React, { useState } from 'react';
import { parseRepoInput } from '../../utils/formatters';
import styles from './SearchBar.module.css';

function SearchBar({
  onAnalyze,
  isLoading,
  placeholder = 'Enter owner/repo (e.g. facebook/react)',
  initialValue = '',
  buttonText = 'Analyze',
}) {
  const [input, setInput] = useState(initialValue);
  const [error, setError] = useState(null);

  function handleSubmit() {
    setError(null);
    const parsed = parseRepoInput(input);
    if (!parsed) {
      setError('Use format: owner/repo — e.g. facebook/react');
      return;
    }
    onAnalyze(parsed.owner, parsed.repo);
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter') handleSubmit();
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.row}>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={isLoading}
          aria-label="Repository input"
          className={`${styles.input} ${error ? styles.inputError : ''}`}
        />
        <button
          onClick={handleSubmit}
          disabled={isLoading || !input.trim()}
          className={styles.btn}
          style={{ backgroundColor: isLoading ? '#6b7280' : '#2563eb' }}
        >
          {isLoading ? 'Loading...' : buttonText}
        </button>
      </div>
      {error && (
        <span role="alert" className={styles.errorMsg}>{error}</span>
      )}
    </div>
  );
}

export default SearchBar;
