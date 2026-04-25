import { COLORS } from '../../theme';

type Tab = 'feed' | 'saved' | 'me';

interface Props {
  active: Tab;
  onNavigate: (tab: Tab) => void;
}

const FeedIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
    <polyline points="9 22 9 12 15 12 15 22" />
  </svg>
);

const SavedIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
  </svg>
);

const MeIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
);

const TABS: { id: Tab; label: string; Icon: React.FC }[] = [
  { id: 'feed', label: 'Feed', Icon: FeedIcon },
  { id: 'saved', label: 'Saved', Icon: SavedIcon },
  { id: 'me', label: 'Me', Icon: MeIcon },
];

export default function NavBar({ active, onNavigate }: Props) {
  return (
    <div
      style={{
        position: 'absolute',
        bottom: 0,
        left: 0,
        right: 0,
        height: 64,
        background: COLORS.surface,
        borderTop: `1px solid ${COLORS.border}`,
        display: 'flex',
        alignItems: 'center',
        zIndex: 100,
      }}
    >
      {TABS.map(({ id, label, Icon }) => (
        <button
          key={id}
          onClick={() => onNavigate(id)}
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 3,
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: active === id ? COLORS.accent : COLORS.textTer,
            fontSize: 10,
            fontWeight: 500,
            letterSpacing: '0.03em',
            transition: 'color 0.15s ease',
            padding: '0 0 4px',
          }}
        >
          <Icon />
          {label}
        </button>
      ))}
    </div>
  );
}
