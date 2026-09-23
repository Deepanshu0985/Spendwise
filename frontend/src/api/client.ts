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
}

interface ApiEnvelope<T> {
  data: T
  meta: unknown
}

const BASE_PATH = '/api/v1'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_PATH}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
    // Cookie sessions (ADR-009): the session cookie must ride along on every request.
    credentials: 'include',
  })

  const body = await response.json()

  if (!response.ok) {
    throw new ApiRequestError((body as { error: ApiErrorBody }).error)
  }

  return (body as ApiEnvelope<T>).data
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
