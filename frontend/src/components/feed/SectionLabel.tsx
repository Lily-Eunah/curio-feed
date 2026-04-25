import { ReactNode } from 'react';
import { COLORS } from '../../theme';

export default function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <div
      style={{
        fontSize: 11,
        fontWeight: 700,
        color: COLORS.textTer,
        letterSpacing: '0.08em',
        textTransform: 'uppercase',
        marginBottom: 8,
      }}
    >
      {children}
    </div>
  );
}
