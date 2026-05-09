import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// @testing-library/react auto-cleanup checks `typeof afterEach === 'function'` at
// module load time. Without `globals: true` in vitest.config.ts, the global is
// undefined at that moment, so cleanup is never registered. Wire it explicitly.
afterEach(cleanup);
