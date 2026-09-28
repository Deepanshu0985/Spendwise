// Single API client layer per docs/09-project/coding-standards.md ("API client layer
// and no duplicated financial calculation logic in the UI"). Every request goes through
// here so the {data, meta} / {error} envelope (docs/03-api/error-contract.md,
// docs/03-api/api-specification.md) is only parsed in one place.

export interface ApiErrorDetail {
  field: string
  reason: string
}

export interface ApiErrorBody {
  code: string
  message: string
  details: ApiErrorDetail[]
  requestId: string
}

export class ApiRequestError extends Error {
  readonly code: string
  readonly details: ApiErrorDetail[]
  readonly requestId: string

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.code = body.code
    this.details = body.details
    this.requestId = body.requestId
  }

  fieldError(field: string): string | undefined {
    return this.details.find((d) => d.field === field)?.reason
  }
}

interface ApiEnvelope<T> {
  data: T
  meta: unknown
}

// Relative '/api/v1' works whenever the frontend and backend share one origin (the
// Vite dev proxy locally, or a single edge reverse proxy in production). A split-origin
// deployment (frontend and backend on different domains, e.g. Vercel + Render) bakes in
// the backend's absolute URL at build time via VITE_API_BASE_URL.
const BASE_PATH = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

// Double-submit CSRF (ADR-009): the backend mints a non-httpOnly csrf_token cookie on
// the first GET and rejects any mutating request whose X-CSRF-Token header doesn't
// match it. The token's value is read from the X-CSRF-Token *response* header (see
// CsrfTokenFilter/CorsConfig on the backend), not from document.cookie - a same-origin
// deployment could read the cookie directly, but a split-origin one (frontend and
// backend on different domains) never can, since cookie access via JS is scoped to the
// cookie's own domain regardless of SameSite/Secure. Reading the response header instead
// works identically in both topologies. Every GET the app makes (starting with the auth
// bootstrap check on load) primes this before the user can ever submit a form.
let cachedCsrfToken: string | null = null

function csrfHeaders(): Record<string, string> {
  return cachedCsrfToken ? { 'X-CSRF-Token': cachedCsrfToken } : {}
}

async function request<T>(path: string, init: RequestInit): Promise<ApiEnvelope<T>> {
  const response = await fetch(`${BASE_PATH}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...csrfHeaders(),
      ...init.headers,
    },
    // Cookie sessions (ADR-009): the session cookie must ride along on every request.
    credentials: 'include',
  })

  const freshToken = response.headers.get('X-CSRF-Token')
  if (freshToken) {
    cachedCsrfToken = freshToken
  }

  if (response.status === 204) {
    return { data: undefined as T, meta: null }
  }

  const body = await response.json()

  if (!response.ok) {
    throw new ApiRequestError((body as { error: ApiErrorBody }).error)
  }

  return body as ApiEnvelope<T>
}

function query(params?: Record<string, string | number | undefined>): string {
  if (!params) return ''
  const entries = Object.entries(params).filter(([, v]) => v !== undefined) as [string, string | number][]
  if (entries.length === 0) return ''
  const search = new URLSearchParams(entries.map(([k, v]) => [k, String(v)]))
  return `?${search.toString()}`
}

export const apiClient = {
  get: <T>(path: string, params?: Record<string, string | number | undefined>) =>
    request<T>(`${path}${query(params)}`, { method: 'GET' }).then((e) => e.data),
  getWithMeta: <T, M>(path: string, params?: Record<string, string | number | undefined>) =>
    request<T>(`${path}${query(params)}`, { method: 'GET' }) as Promise<{ data: T; meta: M }>,
  post: <T>(path: string, body?: unknown, extraHeaders?: Record<string, string>) =>
    request<T>(path, {
      method: 'POST',
      body: body === undefined ? undefined : JSON.stringify(body),
      headers: extraHeaders,
    }).then((e) => e.data),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) }).then(
      (e) => e.data,
    ),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }).then((e) => e.data),
}
