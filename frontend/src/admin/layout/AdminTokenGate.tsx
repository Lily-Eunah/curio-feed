import { useState } from 'react';

interface Props {
  onSubmit: (token: string) => void;
  invalid?: boolean;
}

/**
 * Full-screen gate shown when no admin token is present (or after a 401).
 * The token is verified by the backend on the first real request; an empty
 * submit is ignored.
 */
export default function AdminTokenGate({ onSubmit, invalid = false }: Props) {
  const [value, setValue] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = value.trim();
    if (trimmed) onSubmit(trimmed);
  };

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        width: '100%',
        backgroundColor: '#0f172a',
        padding: '24px',
      }}
    >
      <form
        onSubmit={handleSubmit}
        style={{
          width: '100%',
          maxWidth: '380px',
          backgroundColor: '#1e293b',
          borderRadius: '12px',
          padding: '28px',
          boxShadow: '0 10px 30px rgba(0,0,0,0.3)',
        }}
      >
        <div style={{ fontSize: '18px', fontWeight: 700, color: '#fff', marginBottom: '4px' }}>
          CurioFeed Admin
        </div>
        <p style={{ fontSize: '13px', color: '#94a3b8', marginBottom: '20px' }}>
          Enter the admin token to continue.
        </p>

        <input
          type="password"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="Admin token"
          autoFocus
          aria-label="Admin token"
          style={{
            width: '100%',
            boxSizing: 'border-box',
            padding: '10px 12px',
            borderRadius: '8px',
            border: `1px solid ${invalid ? '#ef4444' : '#334155'}`,
            backgroundColor: '#0f172a',
            color: '#fff',
            fontSize: '14px',
            marginBottom: invalid ? '8px' : '16px',
          }}
        />

        {invalid && (
          <p style={{ fontSize: '12px', color: '#f87171', marginBottom: '16px' }}>
            Invalid token. Please try again.
          </p>
        )}

        <button
          type="submit"
          style={{
            width: '100%',
            padding: '10px 12px',
            borderRadius: '8px',
            border: 'none',
            backgroundColor: '#6366f1',
            color: '#fff',
            fontSize: '14px',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Continue
        </button>
      </form>
    </div>
  );
}
