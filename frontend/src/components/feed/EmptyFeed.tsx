import { COLORS } from '../../theme';

interface Props {
  selectedCategory: string;
  onCategoryChange: (cat: string) => void;
}

export default function EmptyFeed({ selectedCategory, onCategoryChange }: Props) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '60px 24px',
        textAlign: 'center',
      }}
    >
      <div style={{ fontSize: 36, marginBottom: 16 }}>∅</div>
      <div style={{ fontSize: 17, fontWeight: 600, color: COLORS.text, marginBottom: 8 }}>
        No articles found
      </div>
      <div style={{ fontSize: 14, color: COLORS.textSec, lineHeight: 1.5, marginBottom: 24 }}>
        {selectedCategory !== 'All'
          ? `No ${selectedCategory} articles yet.`
          : 'No articles available right now.'}
        {'\n'}Try another category or level.
      </div>
      {selectedCategory !== 'All' && (
        <button
          onClick={() => onCategoryChange('All')}
          style={{
            background: COLORS.text,
            color: '#fff',
            border: 'none',
            borderRadius: 20,
            padding: '10px 20px',
            fontSize: 14,
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Show all categories
        </button>
      )}
    </div>
  );
}
