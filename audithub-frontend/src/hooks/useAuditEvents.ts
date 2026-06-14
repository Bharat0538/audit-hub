import { useQuery } from '@tanstack/react-query';
import { eventsApi } from '../api/events.api';
import type { EventFilters } from '../types';

export function useAuditEvents(
  filters: EventFilters,
  options?: { pageToken?: string; pageSize?: number; enabled?: boolean }
) {
  return useQuery({
    queryKey: ['events', filters, options?.pageToken, options?.pageSize],
    queryFn: () =>
      eventsApi.search({
        ...filters,
        pageToken: options?.pageToken,
        pageSize:  options?.pageSize ?? 50,
      }),
    enabled:        options?.enabled !== false,
    staleTime:      30_000,
  });
}

export function useAuditEvent(
  eventId: string,
  applicationId: string,
  eventTime: string
) {
  return useQuery({
    queryKey: ['event', eventId],
    queryFn: () => eventsApi.getById(eventId, applicationId, eventTime),
    staleTime: 5 * 60_000,
  });
}

export function useEntityHistory(
  resourceType: string,
  resourceId: string,
  options?: { startTime?: string; endTime?: string }
) {
  return useQuery({
    queryKey: ['entity-history', resourceType, resourceId, options],
    queryFn: () => eventsApi.getEntityHistory(resourceType, resourceId, options),
    staleTime: 60_000,
  });
}

export function useUserActivity(
  userId: string,
  params: { startTime: string; endTime: string }
) {
  return useQuery({
    queryKey: ['user-activity', userId, params],
    queryFn: () => eventsApi.getUserActivity(userId, params),
    staleTime: 30_000,
  });
}
