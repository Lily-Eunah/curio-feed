import { ReactNode } from 'react';
import { COLORS } from '../../theme';

interface Props {
  open: boolean;
  onClose: () => void;
  title?: string;
  children: ReactNode;
  maxHeight?: string;
}

export default function BottomSheet({ open, onClose, title, children, maxHeight = '70%' }: Props) {
  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        zIndex: 200,
        pointerEvents: open ? 'all' : 'none',
      }}
    >
      <div
        onClick={onClose}
        style={{
          position: 'absolute',
          inset: 0,
          background: 'rgba(0,0,0,0.4)',
          opacity: open ? 1 : 0,
          transition: 'opacity 0.25s ease',
        }}
      />
      <div
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          background: COLORS.surface,
          borderRadius: '22px 22px 0 0',
          maxHeight,
          overflowY: 'auto',
          transform: open ? 'translateY(0)' : 'translateY(100%)',
          transition: 'transform 0.3s cubic-bezier(0.32,0.72,0,1)',
          boxShadow: '0 -4px 32px rgba(0,0,0,0.12)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'center', padding: '12px 0 4px' }}>
          <div style={{ width: 36, height: 4, borderRadius: 2, background: COLORS.border }} />
        </div>
        {title && (
          <div
            style={{
              padding: '8px 20px 16px',
              fontSize: 16,
              fontWeight: 600,
              color: COLORS.text,
              borderBottom: `1px solid ${COLORS.borderLight}`,
            }}
          >
            {title}
          </div>
        )}
        {children}
      </div>
    </div>
  );
}
