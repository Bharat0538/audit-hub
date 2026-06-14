import { apiClient } from './client';
import type { Application, ApiKey, ApiKeyCreated } from '../types';
import type { UserWithRole } from '../types';

export const applicationsApi = {
  list: () =>
    apiClient.get<ApiResponse<Application[]>>('/applications').then((r) => r.data.data),

  create: (data: { name: string; environment: string; description?: string; webhookUrl?: string }) =>
    apiClient.post<ApiResponse<Application>>('/applications', data).then((r) => r.data.data),

  update: (id: string, data: Partial<Application>) =>
    apiClient.patch<ApiResponse<Application>>(`/applications/${id}`, data).then((r) => r.data.data),

  delete: (id: string) => apiClient.delete(`/applications/${id}`),
};

export const apiKeysApi = {
  list: () =>
    apiClient.get<ApiResponse<ApiKey[]>>('/api-keys').then((r) => r.data.data),

  create: (data: {
    name: string;
    applicationId?: string;
    keyType: 'WRITE' | 'READ' | 'ADMIN';
    scopes?: string[];
    expiresAt?: string;
  }) =>
    apiClient.post<ApiResponse<ApiKeyCreated>>('/api-keys', data).then((r) => r.data.data),

  revoke: (keyId: string) => apiClient.delete(`/api-keys/${keyId}`),
};

export const teamApi = {
  listUsers: () =>
    apiClient.get<ApiResponse<UserWithRole[]>>('/team/users').then((r) => r.data.data),

  invite: (data: { email: string; role: string; applicationId?: string }) =>
    apiClient.post('/team/invite', data),

  updateRole: (userId: string, role: string) =>
    apiClient.patch(`/team/users/${userId}/role`, { role }),

  removeUser: (userId: string) => apiClient.delete(`/team/users/${userId}`),
};

export const alertsApi = {
  listRules: () =>
    apiClient.get<ApiResponse<any[]>>('/alerts/rules').then((r) => r.data.data),

  createRule: (data: unknown) =>
    apiClient.post<ApiResponse<any>>('/alerts/rules', data).then((r) => r.data.data),

  updateRule: (id: string, data: unknown) =>
    apiClient.patch<ApiResponse<any>>(`/alerts/rules/${id}`, data).then((r) => r.data.data),

  deleteRule: (id: string) => apiClient.delete(`/alerts/rules/${id}`),
};

export const replayApi = {
  submit: (data: unknown) =>
    apiClient.post<ApiResponse<any>>('/replay', data).then((r) => r.data.data),

  listJobs: () =>
    apiClient.get<ApiResponse<any[]>>('/replay').then((r) => r.data.data),
};

interface ApiResponse<T> {
  success: boolean;
  data: T;
}
