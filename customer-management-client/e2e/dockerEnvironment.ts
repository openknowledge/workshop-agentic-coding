import type { StartedDockerComposeEnvironment } from 'testcontainers';

let environment: StartedDockerComposeEnvironment | undefined;

export function setEnvironment(env: StartedDockerComposeEnvironment): void {
  environment = env;
}

export function takeEnvironment(): StartedDockerComposeEnvironment | undefined {
  const current = environment;
  environment = undefined;
  return current;
}
