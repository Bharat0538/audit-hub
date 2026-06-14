import { useQuery } from '@tanstack/react-query';
import { eventsApi } from '../api/events.api';
import { useTenantStore } from '../store/tenant.store';
import { subDays } from 'date-fns';

export function useDashboardStats() {
  const selectedAppId = useTenantStore((s) => s.selectedAppId);

  const stats = useQuery({
    queryKey: ['dashboard-stats', selectedAppId],
    queryFn: () => eventsApi.getDashboardStats(selectedAppId ?? undefined),
    refetchInterval: 30_000,
    staleTime: 20_000,
  });

  const trends = useQuery({
    queryKey: ['event-trends', selectedAppId],
    queryFn: () =>
      eventsApi.getTrends({
        applicationId: selectedAppId ?? undefined,
        granularity:   'DAY',
        startTime:     subDays(new Date(), 30).toISOString(),
        endTime:       new Date().toISOString(),
      }),
    refetchInterval: 60_000,
    staleTime: 55_000,
  });

  return { stats, trends };
}
