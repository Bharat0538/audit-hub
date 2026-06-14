import { apiClient } from './client';
import type { AuditEvent, AuditEventPage, EventFilters, DashboardStats, TrendPoint } from '../types';

export const eventsApi = {
  search: (filters: EventFilters & { pageToken?: string; pageSize?: number }) =>
    apiClient.get<ApiResponse<AuditEventPage>>('/events', { params: flattenFilters(filters) })
      .then((r) => r.data.data),

  getById: (eventId: string, applicationId: string, eventTime: string) =>
    apiClient.get<ApiResponse<AuditEvent>>(`/events/${eventId}`, {
      params: { applicationId, eventTime },
    }).then((r) => r.data.data),

  getEntityHistory: (
    resourceType: string,
    resourceId: string,
    params?: { startTime?: string; endTime?: string; pageToken?: string; pageSize?: number }
  ) =>
    apiClient.get<ApiResponse<AuditEventPage>>(`/events/entity/${resourceType}/${resourceId}`, { params })
      .then((r) => r.data.data),

  getUserActivity: (
    userId: string,
    params: { startTime: string; endTime: string; pageToken?: string; pageSize?: number }
  ) =>
    apiClient.get<ApiResponse<AuditEventPage>>(`/events/user/${userId}`, { params }).then((r) => r.data.data),

  getDashboardStats: (applicationId?: string) =>
    apiClient.get<ApiResponse<DashboardStats>>('/dashboard/stats', {
      params: applicationId ? { applicationId } : {},
    }).then((r) => r.data.data),

  getTrends: (params: {
    applicationId?: string;
    granularity?: 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';
    startTime: string;
    endTime: string;
  }) =>
    apiClient.get<ApiResponse<{ dataPoints: TrendPoint[] }>>('/dashboard/trends', { params })
      .then((r) => r.data.data),
};

function flattenFilters(filters: EventFilters & { pageToken?: string; pageSize?: number }) {
  const params: Record<string, unknown> = { ...filters };
  if (filters.actionTypes?.length) params.actionTypes = filters.actionTypes.join(',');
  if (filters.severities?.length)  params.severities   = filters.severities.join(',');
  if (filters.outcomes?.length)    params.outcomes     = filters.outcomes.join(',');
  if (filters.tags?.length)        params.tags        = filters.tags.join(',');
  return params;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
}
