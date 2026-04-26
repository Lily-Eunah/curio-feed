export const COLORS = {
  bg: '#FAFAF8',
  surface: '#FFFFFF',
  border: '#E8E5DE',
  borderLight: '#F0EDE6',
  text: '#1C1A16',
  textSec: '#7A7670',
  textTer: '#B0ACA5',
  accent: 'oklch(0.58 0.12 42)',
  accentLight: 'oklch(0.95 0.03 42)',
  accentMid: 'oklch(0.88 0.06 42)',
  easy: 'oklch(0.50 0.13 145)',
  easyBg: 'oklch(0.95 0.04 145)',
  medium: 'oklch(0.56 0.14 75)',
  mediumBg: 'oklch(0.95 0.05 75)',
  hard: 'oklch(0.52 0.14 28)',
  hardBg: 'oklch(0.95 0.05 28)',
} as const;

export const CAT_COLORS: Record<string, { dot: string; bg: string }> = {
  All:      { dot: '#9CA3AF', bg: '#F3F4F6' },
  Tech:     { dot: 'oklch(0.52 0.15 260)', bg: 'oklch(0.95 0.03 260)' },
  Science:  { dot: 'oklch(0.50 0.13 165)', bg: 'oklch(0.95 0.04 165)' },
  Business: { dot: 'oklch(0.55 0.14 75)',  bg: 'oklch(0.95 0.04 75)' },
  Culture:  { dot: 'oklch(0.52 0.14 310)', bg: 'oklch(0.95 0.04 310)' },
};

export const LEVEL_CONFIG: Record<string, { label: string; color: string; bg: string; desc: string }> = {
  EASY:   { label: 'Easy',   color: COLORS.easy,   bg: COLORS.easyBg,   desc: 'Accessible language, clear structure' },
  MEDIUM: { label: 'Medium', color: COLORS.medium, bg: COLORS.mediumBg, desc: 'Moderate complexity, varied vocabulary' },
  HARD:   { label: 'Hard',   color: COLORS.hard,   bg: COLORS.hardBg,   desc: 'Academic style, nuanced arguments' },
};

export const CATEGORIES = ['All', 'Tech', 'Science', 'Business', 'Culture'] as const;
