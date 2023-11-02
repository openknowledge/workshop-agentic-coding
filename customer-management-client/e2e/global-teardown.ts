import { takeEnvironment } from './dockerEnvironment';

export default async function globalTeardown(): Promise<void> {
  const environment = takeEnvironment();
  if (!environment) {
    console.log(
      '[playwright] No running testcontainers environment, skipping docker compose down.'
    );
    return;
  }

  console.log('[playwright] Shutting down docker compose stack...');
  try {
    await environment.down({ removeVolumes: true });
  } catch (err) {
    console.error('[playwright] docker compose down failed:', err);
  }
}
