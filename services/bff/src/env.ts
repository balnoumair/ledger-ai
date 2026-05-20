import 'dotenv/config';

export interface Env {
  backendUrl: string;
  bffPort: number;
  corsOrigins: string[];
  logLevel: string;
}

export const env: Env = {
  backendUrl: process.env.BACKEND_URL ?? 'http://localhost:8080',
  bffPort: Number(process.env.BFF_PORT ?? '8787'),
  corsOrigins: (process.env.CORS_ORIGINS ?? 'http://localhost:5173')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean),
  logLevel: process.env.LOG_LEVEL ?? 'info',
};
