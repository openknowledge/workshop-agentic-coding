import '@testing-library/jest-dom';
import { afterAll } from 'vitest';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { randomUUID } from 'node:crypto';

declare global {
  interface Window {
    __coverage__?: Record<string, unknown>;
  }
}

if (process.env.VITE_COVERAGE) {
  afterAll(() => {
    const coverage = window.__coverage__;
    if (!coverage) return;
    const outDir = join(dirname(fileURLToPath(import.meta.url)), '../.nyc_output');
    mkdirSync(outDir, { recursive: true });
    writeFileSync(join(outDir, `vitest-${randomUUID()}.json`), JSON.stringify(coverage));
  });
}
