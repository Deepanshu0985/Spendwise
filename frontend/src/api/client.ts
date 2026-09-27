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

const BASE_PATH = '/api/v1'

// Double-submit CSRF (ADR-009): the backend mints a non-httpOnly csrf_token
// cookie on the first GET and rejects any mutating request whose
// X-CSRF-Token header doesn't match it. Every GET the app makes (starting
// with the auth bootstrap check on load) primes this cookie before the user
// can ever submit a form, so no separate "prime" call is needed here.
function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[1]) : null
}

function csrfHeaders(): Record<string, string> {
  const token = readCookie('csrf_token')
  return token ? { 'X-CSRF-Token': token } : {}
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
