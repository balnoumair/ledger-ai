import { describe, it, expect, vi } from 'vitest';
import { createApp } from './app.js';

function jsonRes(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}

function makeApp(stubBackend: (req: Request) => Promise<Response> | Response) {
  const fetchImpl = vi.fn(
    async (input: string | URL | Request, init?: RequestInit): Promise<Response> => {
      const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
      const req = new Request(url, init);
      return Promise.resolve(stubBackend(req));
    },
  ) as unknown as typeof fetch;

  const app = createApp({
    backendUrl: 'http://backend.test',
    corsOrigins: ['http://localhost:5173'],
    fetchImpl,
  });
  return { app, fetchImpl };
}

describe('BFF routes', () => {
  it('GET /healthz returns ok', async () => {
    const { app } = makeApp(() => jsonRes(404, {}));
    const res = await app.request('/healthz');
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ ok: true });
  });

  it('POST /api/plaid/link-token forwards to backend', async () => {
    const { app, fetchImpl } = makeApp(() => jsonRes(200, { link_token: 'lt-1', expiration: null }));
    const res = await app.request('/api/plaid/link-token', { method: 'POST' });
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ link_token: 'lt-1', expiration: null });
    expect(fetchImpl).toHaveBeenCalledOnce();
    const [calledUrl] = (fetchImpl as unknown as { mock: { calls: [string][] } }).mock.calls[0]!;
    expect(calledUrl).toBe('http://backend.test/internal/plaid/link-token');
  });

  it('POST /api/plaid/exchange rejects empty body', async () => {
    const { app, fetchImpl } = makeApp(() => jsonRes(200, []));
    const res = await app.request('/api/plaid/exchange', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect(res.status).toBe(400);
    expect(fetchImpl).not.toHaveBeenCalled();
  });

  it('POST /api/plaid/exchange forwards valid body', async () => {
    const { app, fetchImpl } = makeApp(() =>
      jsonRes(200, [{ id: 'a1', name: 'Checking', institution_name: 'First Platypus Bank' }]),
    );
    const res = await app.request('/api/plaid/exchange', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({
        public_token: 'pub-1',
        institution: { name: 'First Platypus Bank', institution_id: 'ins_109508' },
      }),
    });
    expect(res.status).toBe(200);
    expect(fetchImpl).toHaveBeenCalledOnce();
  });

  it('GET /api/accounts forwards', async () => {
    const { app } = makeApp(() => jsonRes(200, []));
    const res = await app.request('/api/accounts');
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual([]);
  });

  it('PATCH /api/accounts/:id validates body and forwards', async () => {
    const { app, fetchImpl } = makeApp(() => jsonRes(200, { id: 'x', included: false }));
    const bad = await app.request('/api/accounts/123', {
      method: 'PATCH',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ included: 'no' }),
    });
    expect(bad.status).toBe(400);

    const good = await app.request('/api/accounts/123', {
      method: 'PATCH',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ included: false }),
    });
    expect(good.status).toBe(200);
    expect(fetchImpl).toHaveBeenCalledOnce();
  });
});
