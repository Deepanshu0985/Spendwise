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

// The app always calls its own address (/api/v1): the Vite dev proxy does the forwarding locally and, on Vercel, the rewrite
// rules in vercel.json forward /api to the right backend (live or staging, by which site you are on). Keeping the browser on one
// origin is deliberate: with the backend on a different domain (onrender.com) its login cookie is a "third-party" cookie, and
// iPhone browsers (Safari and Chrome both) refuse it, so login quietly failed on phones while working on laptops.
const BASE_PATH = '/api/v1'

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

// How many calls to the server are in flight right now, so one thin progress bar at the top of the page can show that the app
// is working on something (saving, deleting, loading) without every button having to draw its own indicator.
let inFlight = 0
const inFlightListeners = new Set<() => void>()

function trackInFlight<T>(work: () => Promise<T>): Promise<T> {
  inFlight += 1
  inFlightListeners.forEach((listener) => listener())
  return work().finally(() => {
    inFlight -= 1
    inFlightListeners.forEach((listener) => listener())
  })
}

export function subscribeInFlight(listener: () => void): () => void {
  inFlightListeners.add(listener)
  return () => inFlightListeners.delete(listener)
}

export function getInFlight(): number {
  return inFlight
}

async function request<T>(path: string, init: RequestInit): Promise<ApiEnvelope<T>> {
  return trackInFlight(() => doRequest<T>(path, init))
}

async function doRequest<T>(path: string, init: RequestInit): Promise<ApiEnvelope<T>> {
  // FormData (multipart uploads) must never get an explicit Content-Type - the browser
  // sets one itself with the correct random boundary; overriding it breaks the upload.
  const isFormData = init.body instanceof FormData
  const response = await fetch(`${BASE_PATH}${path}`, {
    ...init,
    headers: {
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
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

/** A file the server sends back (the data export): the raw bytes, with the same cookie and error handling as every call. */
async function download(path: string): Promise<Blob> {
  return trackInFlight(() => doDownload(path))
}

async function doDownload(path: string): Promise<Blob> {
  const response = await fetch(`${BASE_PATH}${path}`, { method: 'GET', credentials: 'include', headers: csrfHeaders() })
  const freshToken = response.headers.get('X-CSRF-Token')
  if (freshToken) cachedCsrfToken = freshToken
  if (!response.ok) {
    const body = (await response.json()) as { error: ApiErrorBody }
    throw new ApiRequestError(body.error)
  }
  return response.blob()
}

export const apiClient = {
  download,
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
  postForm: <T>(path: string, form: FormData) => request<T>(path, { method: 'POST', body: form }).then((e) => e.data),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) }).then(
      (e) => e.data,
    ),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }).then((e) => e.data),
}
