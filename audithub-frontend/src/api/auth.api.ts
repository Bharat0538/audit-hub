import { apiClient } from './client';
import type { AuthTokens, LoginRequest, SignupRequest } from '../types';
import type { Organization } from '../types';

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<ApiResponse<AuthTokens>>('/auth/login', data).then((r) => r.data.data),

  signup: (data: SignupRequest) =>
    apiClient.post<ApiResponse<{ organizationId: string; userId: string; plainApiKey: string }>>(
      '/organizations', data
    ).then((r) => r.data.data),

  logout: () => apiClient.post('/auth/logout'),

  refreshToken: (refreshToken: string) =>
    apiClient.post<ApiResponse<{ accessToken: string; expiresIn: number }>>(
      '/auth/refresh', { refreshToken }
    ).then((r) => r.data.data),

  forgotPassword: (email: string) =>
    apiClient.post('/auth/forgot-password', { email }),

  resetPassword: (token: string, newPassword: string) =>
    apiClient.post('/auth/reset-password', { token, newPassword }),

  verifyEmail: (token: string) =>
    apiClient.post<ApiResponse<AuthTokens>>('/auth/verify-email', { token }).then((r) => r.data.data),

  initiateSaml: (orgSlug: string) =>
    apiClient.get<ApiResponse<{ redirectUrl: string }>>(`/auth/saml/initiate?orgSlug=${orgSlug}`).then((r) => r.data.data),

  setupMfa: () =>
    apiClient.post<ApiResponse<{ secret: string; qrCodeUrl: string; backupCodes: string[] }>>(
      '/auth/mfa/setup'
    ).then((r) => r.data.data),

  confirmMfa: (code: string) => apiClient.post('/auth/mfa/confirm', { code }),
};

export const organizationApi = {
  getCurrent: () =>
    apiClient.get<ApiResponse<Organization>>('/organizations/me').then((r) => r.data.data),

  update: (data: Partial<Organization>) =>
    apiClient.patch<ApiResponse<Organization>>('/organizations/me', data).then((r) => r.data.data),

  getUsage: () =>
    apiClient.get('/organizations/me/usage').then((r) => r.data.data),
};

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: unknown;
}
