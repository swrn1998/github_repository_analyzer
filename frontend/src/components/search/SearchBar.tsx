import React, { useState } from 'react';
import { parseRepoInput } from '../../utils/formatters';

interface SearchBarProps {
  onAnalyze: (owner: string, repo: string) => void;
  isLoading: boolean;
  placeholder?: string;
  initialValue?: string;
  buttonText?: string;
}

/**
 * Input component for entering and submitting a GitHub repository for analysis.
 *
 * Accepts "owner/repo" format, validates it client-side, and splits it into
 * owner and repo before calling onAnalyze.
 *
 * Pressing Enter triggers the same action as clicking the button.
 */
const SearchBar: React.FC<SearchBarProps> = ({
  onAnalyze,
  isLoading,
  placeholder = 'Enter owner/repo (e.g. facebook/react)',
  initialValue = '',
  buttonText = 'Analyze',
}) => {
  const [input, setInput] = useState(initialValue);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = () => {
    setError(null);
    const parsed = parseRepoInput(input);
    if (!parsed) {
      setError('Please enter a valid "owner/repo" format (e.g., facebook/react)');
      return;
    }
    onAnalyze(parsed.owner, parsed.repo);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleSubmit();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', width: '100%' }}>
      <div style={{ display: 'flex', gap: '8px' }}>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={isLoading}
          aria-label="Repository input"
          style={{
            flex: 1,
            padding: '10px 14px',
            fontSize: '16px',
            border: `1px solid ${error ? '#ef4444' : '#d1d5db'}`,
            borderRadius: '8px',
            outline: 'none',
            opacity: isLoading ? 0.7 : 1,
          }}
        />
        <button
          onClick={handleSubmit}
          disabled={isLoading || !input.trim()}
          aria-label="Analyze repository"
          style={{
            padding: '10px 20px',
            backgroundColor: isLoading ? '#6b7280' : '#2563eb',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            cursor: isLoading || !input.trim() ? 'not-allowed' : 'pointer',
            fontSize: '16px',
            fontWeight: 600,
            whiteSpace: 'nowrap',
          }}
        >
          {isLoading ? 'Loading...' : buttonText}
        </button>
      </div>
      {error && (
        <span role="alert" style={{ color: '#ef4444', fontSize: '14px' }}>
          {error}
        </span>
      )}
    </div>
  );
};

export default SearchBar;
