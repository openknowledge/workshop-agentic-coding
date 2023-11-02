import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import { DockerComposeEnvironment, Wait } from 'testcontainers';
import { setEnvironment } from './dockerEnvironment';

const REPO_ROOT = resolve(fileURLToPath(new URL('../..', import.meta.url)), '.');
const LOCAL_BACKEND_URL = 'http://localhost:8080';
const REQUEST_TIMEOUT_MS = 2_000;
const STARTUP_TIMEOUT_MS = 360_000;
// testcontainers indexes containers as `<service>-<index>`, not by the plain service name.
// See note in DockerComposeEnvironment.warnForUnusedWaitStrategies.
const BACKEND_CONTAINER = 'customer-management-server-1';
const BACKEND_CONTAINER_PORT = 8080;

async function isBackendReachable(url: string): Promise<boolean> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    const response = await fetch(url, { signal: controller.signal });
    return response.ok;
  } catch {
    return false;
  } finally {
    clearTimeout(timer);
  }
}

// Playwright starts `webServer` (vite) BEFORE `globalSetup` runs;
// VITE_BACKEND_ENDPOINT must therefore already be set when the process starts
// (local: Vite default `http://127.0.0.1:8080`; CI: via job variable in `.gitlab-ci.yml`).
// `globalSetup` itself can no longer propagate the value to the webServer child process.
export default async function globalSetup(): Promise<void> {
  if (process.env.PLAYWRIGHT_SKIP_DOCKER === 'true') {
    if (!(await isBackendReachable(`${LOCAL_BACKEND_URL}/`))) {
      throw new Error(
        `[playwright] PLAYWRIGHT_SKIP_DOCKER=true, but backend is not reachable at ${LOCAL_BACKEND_URL}. ` +
          'Start the docker compose stack manually or run the test script without :dev suffix.'
      );
    }
    console.log('[playwright] PLAYWRIGHT_SKIP_DOCKER=true, using existing backend.');
    return;
  }

  console.log('[playwright] Starting docker compose stack via testcontainers...');
  const environment = await new DockerComposeEnvironment(REPO_ROOT, 'docker-compose.yaml')
    .withStartupTimeout(STARTUP_TIMEOUT_MS)
    .withWaitStrategy(BACKEND_CONTAINER, Wait.forHttp('/', BACKEND_CONTAINER_PORT))
    .up();
  setEnvironment(environment);

  const server = environment.getContainer(BACKEND_CONTAINER);
  const backendUrl = `http://${server.getHost()}:${server.getMappedPort(BACKEND_CONTAINER_PORT)}`;
  console.log(`[playwright] Backend reachable at ${backendUrl}`);
}
