import { test as base, expect } from '@playwright/test';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { randomUUID } from 'node:crypto';

declare global {
  interface Window {
    __coverage__: Record<string, unknown> | undefined;
    __saveCoverage__: ((data: string) => void) | undefined;
  }
}

const COVERAGE_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', '.nyc_output');

export const test = base.extend<{ collectCoverage: void }>({
  collectCoverage: [
    async ({ page }, use) => {
      if (!process.env.PLAYWRIGHT_COVERAGE) {
        await use();
        return;
      }

      const coverageSnapshots: string[] = [];

      await page.exposeFunction('__saveCoverage__', (data: string) => {
        coverageSnapshots.push(data);
      });

      // Capture coverage before each full-page navigation via pagehide event
      await page.addInitScript(() => {
        window.addEventListener('pagehide', () => {
          if (window.__coverage__ && window.__saveCoverage__) {
            window.__saveCoverage__(JSON.stringify(window.__coverage__));
          }
        });
      });

      await use();

      const finalCoverage = await page.evaluate(() => window.__coverage__).catch(() => undefined);
      if (finalCoverage) {
        coverageSnapshots.push(JSON.stringify(finalCoverage));
      }

      if (coverageSnapshots.length === 0) {
        return;
      }

      mkdirSync(COVERAGE_DIR, { recursive: true });
      for (const snapshot of coverageSnapshots) {
        writeFileSync(join(COVERAGE_DIR, `playwright-${randomUUID()}.json`), snapshot);
      }
    },
    { auto: true },
  ],
});

export { expect };
