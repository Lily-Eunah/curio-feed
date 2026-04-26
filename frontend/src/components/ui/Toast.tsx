import { COLORS } from '../../theme';

interface Props {
  message: string;
  visible: boolean;
}

export default function Toast({ message, visible }: Props) {
  return (
    <div
      style={{
        position: 'absolute',
        bottom: 76,
        left: '50%',
        transform: `translateX(-50%) translateY(${visible ? 0 : 12}px)`,
        opacity: visible ? 1 : 0,
        transition: 'all 0.22s cubic-bezier(0.34,1.56,0.64,1)',
        background: COLORS.text,
        color: '#fff',
        padding: '9px 16px',
        borderRadius: 20,
        fontSize: 13,
        fontWeight: 500,
        boxShadow: '0 4px 16px rgba(0,0,0,0.18)',
        zIndex: 300,
        whiteSpace: 'nowrap',
        pointerEvents: 'none',
      }}
    >
      {message}
    </div>
  );
}
