import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import { execFileSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { userInfo } from 'node:os';
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

function commandExists(command: string): boolean {
  try {
    execFileSync(command, ['--version'], { stdio: 'ignore' });
    return true;
  } catch {
    return false;
  }
}

function resolvePodmanDockerHost(): string | undefined {
  // Windows Podman machines expose a named pipe, not a Unix socket; `PodmanSocket.Path`
  // there holds an unusable forwarded Windows path, so we must use `PodmanPipe.Path` instead.
  if (process.platform === 'win32') {
    try {
      const pipePath = execFileSync(
        'podman',
        ['machine', 'inspect', '--format', '{{.ConnectionInfo.PodmanPipe.Path}}'],
        { encoding: 'utf-8' }
      ).trim();
      return pipePath ? `npipe://${pipePath}` : undefined;
    } catch {
      return undefined;
    }
  }

  // Rootless Linux exposes the socket directly; macOS runs Podman inside a VM,
  // whose forwarded host-side socket path only `podman machine inspect` knows.
  const rootlessSocketPath = `/run/user/${userInfo().uid}/podman/podman.sock`;
  if (existsSync(rootlessSocketPath)) {
    return `unix://${rootlessSocketPath}`;
  }
  try {
    const socketPath = execFileSync(
      'podman',
      ['machine', 'inspect', '--format', '{{.ConnectionInfo.PodmanSocket.Path}}'],
      {
        encoding: 'utf-8',
      }
    ).trim();
    return socketPath ? `unix://${socketPath}` : undefined;
  } catch {
    return undefined;
  }
}

// testcontainers-node always spawns the literal `docker` binary and never auto-detects
// Podman's socket, so on Docker-less machines we point it at Podman ourselves.
function resolveComposeExecutable(): { executablePath: string } | undefined {
  if (commandExists('docker') || !commandExists('podman')) {
    return undefined;
  }
  if (!process.env.DOCKER_HOST) {
    const dockerHost = resolvePodmanDockerHost();
    if (dockerHost) {
      process.env.DOCKER_HOST = dockerHost;
    }
  }
  // Ryuk bind-mounts the socket into its own container to clean up on exit, which fails
  // for Podman Machine (macOS/Windows) because that socket path isn't shared into the VM.
  // testcontainers-java disables Ryuk automatically for Podman; testcontainers-node doesn't.
  if (!process.env.TESTCONTAINERS_RYUK_DISABLED) {
    process.env.TESTCONTAINERS_RYUK_DISABLED = 'true';
  }
  return { executablePath: 'podman' };
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
  const executable = resolveComposeExecutable();
  if (executable) {
    console.log(
      `[playwright] Docker not found, falling back to Podman (${executable.executablePath}).`
    );
  }
  const environment = await new DockerComposeEnvironment(REPO_ROOT, 'docker-compose.yaml')
    .withStartupTimeout(STARTUP_TIMEOUT_MS)
    .withWaitStrategy(BACKEND_CONTAINER, Wait.forHttp('/', BACKEND_CONTAINER_PORT))
    .withClientOptions(executable ? { executable } : {})
    .up();
  setEnvironment(environment);

  const server = environment.getContainer(BACKEND_CONTAINER);
  const backendUrl = `http://${server.getHost()}:${server.getMappedPort(BACKEND_CONTAINER_PORT)}`;
  console.log(`[playwright] Backend reachable at ${backendUrl}`);
}
