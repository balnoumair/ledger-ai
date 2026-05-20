import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { zValidator } from '@hono/zod-validator';
import { z } from 'zod';
import { env } from './env.js';
import { logger } from './logger.js';

export interface AppDeps {
  backendUrl: string;
  corsOrigins: string[];
  fetchImpl?: typeof fetch;
}

const exchangeBodySchema = z.object({
  public_token: z.string().min(1),
  institution: z
    .object({
      name: z.string().nullish(),
      institution_id: z.string().nullish(),
    })
    .passthrough()
    .nullable()
    .optional(),
});

const updateIncludedSchema = z.object({
  included: z.boolean(),
});

export function createApp(deps: AppDeps = { backendUrl: env.backendUrl, corsOrigins: env.corsOrigins }) {
  const app = new Hono();
  const doFetch = deps.fetchImpl ?? fetch;

  app.use(
    '*',
    cors({
      origin: deps.corsOrigins,
      allowMethods: ['GET', 'POST', 'PATCH', 'OPTIONS'],
      allowHeaders: ['Content-Type'],
      credentials: false,
    }),
  );

  app.use('*', async (c, next) => {
    const start = Date.now();
    const safeToLogBody = !c.req.path.endsWith('/api/plaid/exchange');
    await next();
    const dur = Date.now() - start;
    if (safeToLogBody) {
      logger.info(
        { method: c.req.method, path: c.req.path, status: c.res.status, durMs: dur },
        'request',
      );
    } else {
      logger.info(
        { method: c.req.method, path: c.req.path, status: c.res.status, durMs: dur, redacted: true },
        'request',
      );
    }
  });

  app.get('/healthz', (c) => c.json({ ok: true }));

  app.post('/api/plaid/link-token', async () => {
    const res = await doFetch(`${deps.backendUrl}/internal/plaid/link-token`, { method: 'POST' });
    const body = await res.text();
    return new Response(body, {
      status: res.status,
      headers: { 'content-type': res.headers.get('content-type') ?? 'application/json' },
    });
  });

  app.post('/api/plaid/exchange', zValidator('json', exchangeBodySchema), async (c) => {
    const body = c.req.valid('json');
    const res = await doFetch(`${deps.backendUrl}/internal/plaid/exchange`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(body),
    });
    const text = await res.text();
    return new Response(text, {
      status: res.status,
      headers: { 'content-type': res.headers.get('content-type') ?? 'application/json' },
    });
  });

  app.get('/api/accounts', async () => {
    const res = await doFetch(`${deps.backendUrl}/internal/accounts`);
    const text = await res.text();
    return new Response(text, {
      status: res.status,
      headers: { 'content-type': res.headers.get('content-type') ?? 'application/json' },
    });
  });

  app.patch('/api/accounts/:id', zValidator('json', updateIncludedSchema), async (c) => {
    const id = c.req.param('id');
    const body = c.req.valid('json');
    const res = await doFetch(`${deps.backendUrl}/internal/accounts/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(body),
    });
    const text = await res.text();
    return new Response(text, {
      status: res.status,
      headers: { 'content-type': res.headers.get('content-type') ?? 'application/json' },
    });
  });

  app.onError((err, c) => {
    logger.error({ err: err.message, stack: err.stack }, 'unhandled');
    return c.json({ error: 'internal_error', message: 'Unexpected BFF error' }, 500);
  });

  return app;
}
