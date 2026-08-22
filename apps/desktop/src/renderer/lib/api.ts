const BFF_URL = (import.meta.env.VITE_BFF_URL as string | undefined) ?? 'http://localhost:8787';

export interface LinkTokenResponse {
  link_token: string;
  expiration: string | null;
}

export interface Account {
  id: string;
  plaid_account_id: string;
  name: string;
  official_name: string | null;
  mask: string | null;
  type: string;
  subtype: string | null;
  current_balance: number | null;
  available_balance: number | null;
  iso_currency_code: string | null;
  included: boolean;
  institution_name: string;
}

export interface PlaidInstitutionMeta {
  name: string | null;
  institution_id: string | null;
}

async function jsonOrThrow<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let body: unknown;
    try {
      body = await res.json();
    } catch {
      body = await res.text();
    }
    throw new ApiError(res.status, body);
  }
  return (await res.json()) as T;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: unknown,
  ) {
    super(`API error ${status}`);
  }
}

export async function createLinkToken(): Promise<LinkTokenResponse> {
  const res = await fetch(`${BFF_URL}/api/plaid/link-token`, { method: 'POST' });
  return jsonOrThrow<LinkTokenResponse>(res);
}

export async function exchangePublicToken(
  publicToken: string,
  institution: PlaidInstitutionMeta | null,
): Promise<Account[]> {
  const res = await fetch(`${BFF_URL}/api/plaid/exchange`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ public_token: publicToken, institution }),
  });
  return jsonOrThrow<Account[]>(res);
}

export async function listAccounts(): Promise<Account[]> {
  const res = await fetch(`${BFF_URL}/api/accounts`);
  return jsonOrThrow<Account[]>(res);
}

export async function updateAccountIncluded(id: string, included: boolean): Promise<Account> {
  const res = await fetch(`${BFF_URL}/api/accounts/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ included }),
  });
  return jsonOrThrow<Account>(res);
}

export interface Transaction {
  id: string;
  account_id: string;
  account_name: string;
  name: string;
  merchant_name: string | null;
  /** Plaid convention: positive = money out, negative = money in. */
  amount: number;
  iso_currency_code: string | null;
  date: string;
  pending: boolean;
  category: string | null;
  category_detailed: string | null;
  payment_channel: string | null;
}

export interface TransactionsSyncResult {
  added: number;
  modified: number;
  removed: number;
  total: number;
}

export interface CategorySpend {
  category: string;
  amount: number;
}

export interface Summary {
  net_worth: number;
  total_assets: number;
  total_liabilities: number;
  account_count: number;
  month_spending: number;
  month_income: number;
  spending_by_category: CategorySpend[];
  month: string;
}

export async function refreshBalances(): Promise<Account[]> {
  const res = await fetch(`${BFF_URL}/api/accounts/refresh`, { method: 'POST' });
  return jsonOrThrow<Account[]>(res);
}

export async function syncTransactions(): Promise<TransactionsSyncResult> {
  const res = await fetch(`${BFF_URL}/api/transactions/sync`, { method: 'POST' });
  return jsonOrThrow<TransactionsSyncResult>(res);
}

export async function listTransactions(limit = 50): Promise<Transaction[]> {
  const res = await fetch(`${BFF_URL}/api/transactions?limit=${limit}`);
  return jsonOrThrow<Transaction[]>(res);
}

export async function getSummary(): Promise<Summary> {
  const res = await fetch(`${BFF_URL}/api/summary`);
  return jsonOrThrow<Summary>(res);
}
