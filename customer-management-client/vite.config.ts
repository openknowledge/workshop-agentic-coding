/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import istanbul from 'vite-plugin-istanbul';

// https://vite.dev/config/
export default defineConfig({
  server: {
    host: true,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_ENDPOINT || 'http://127.0.0.1:8080',
        changeOrigin: true,
        secure: false,
      },
    },
    cors: {
      origin: ['http://127.0.0.1:8080/*'],
    },
  },
  plugins: [
    react(),
    ...(process.env.VITE_COVERAGE
      ? [
          istanbul({
            include: 'src/**',
            exclude: ['node_modules', 'tests/', 'src/main.tsx'],
            extension: ['.ts', '.tsx'],
            requireEnv: false,
          }),
        ]
      : []),
  ],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './tests/setup.ts',
    include: ['tests/**/*.test.{ts,tsx}'],
    css: false,
  },
});
