import { useQuery } from '@tanstack/react-query';
import { applicationsApi } from '../api/organizations.api';
import { useTenantStore } from '../store/tenant.store';
import { useEffect } from 'react';

export function useTenant() {
  const { selectedAppId, applications, setSelectedApp, setApplications } = useTenantStore();

  const appsQuery = useQuery({
    queryKey: ['applications'],
    queryFn:  applicationsApi.list,
    staleTime: 5 * 60_000,
  });

  useEffect(() => {
    if (appsQuery.data) {
      setApplications(appsQuery.data);
      if (!selectedAppId && appsQuery.data.length > 0) {
        setSelectedApp(appsQuery.data[0].id);
      }
    }
  }, [appsQuery.data, selectedAppId, setApplications, setSelectedApp]);

  return {
    selectedAppId,
    selectedApp: applications.find((a) => a.id === selectedAppId) ?? null,
    applications,
    setSelectedApp,
    isLoading: appsQuery.isLoading,
  };
}
