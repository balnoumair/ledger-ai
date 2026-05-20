import { serve } from '@hono/node-server';
import { createApp } from './app.js';
import { env } from './env.js';
import { logger } from './logger.js';

const app = createApp();

serve({ fetch: app.fetch, port: env.bffPort }, (info) => {
  logger.info(
    { port: info.port, backendUrl: env.backendUrl, corsOrigins: env.corsOrigins },
    'BFF listening',
  );
});
