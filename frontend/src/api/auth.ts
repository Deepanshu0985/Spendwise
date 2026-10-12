import { apiClient } from './client'
import type { UserProfile } from './types'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
}

export interface GoogleConfig {
  enabled: boolean
  clientId: string | null
}

export interface DeleteAccountRequest {
  confirmation: string
  password?: string
  googleCredential?: string
}

export const authApi = {
  register: (body: RegisterRequest) => apiClient.post<UserProfile>('/auth/register', body),
  login: (body: LoginRequest) => apiClient.post<UserProfile>('/auth/login', body),
  googleConfig: () => apiClient.get<GoogleConfig>('/auth/google/config'),
  loginWithGoogle: (credential: string) => apiClient.post<UserProfile>('/auth/google', { credential }),
  exportData: () => apiClient.download('/users/me/export'),
  deleteAccount: (body: DeleteAccountRequest) => apiClient.post<void>('/users/me/delete', body),
  revokeAllSessions: () => apiClient.delete<void>('/auth/sessions'),
  logout: () => apiClient.post<void>('/auth/logout'),
  me: () => apiClient.get<UserProfile>('/users/me'),
  requestPasswordReset: (email: string) => apiClient.post<void>('/auth/password-reset', { email }),
  confirmPasswordReset: (token: string, password: string) =>
    apiClient.post<void>('/auth/password-reset/confirm', { token, password }),
}
