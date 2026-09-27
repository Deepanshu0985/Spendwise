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

export const authApi = {
  register: (body: RegisterRequest) => apiClient.post<UserProfile>('/auth/register', body),
  login: (body: LoginRequest) => apiClient.post<UserProfile>('/auth/login', body),
  logout: () => apiClient.post<void>('/auth/logout'),
  me: () => apiClient.get<UserProfile>('/users/me'),
  requestPasswordReset: (email: string) => apiClient.post<void>('/auth/password-reset', { email }),
  confirmPasswordReset: (token: string, password: string) =>
    apiClient.post<void>('/auth/password-reset/confirm', { token, password }),
}
