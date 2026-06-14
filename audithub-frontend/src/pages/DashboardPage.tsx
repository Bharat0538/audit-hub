import React from 'react';
import { AlertTriangle } from 'lucide-react';
import { useDashboardStats } from '../hooks/useDashboardStats';
import { StatsCards } from '../components/dashboard/StatsCards';
import { EventTrendChart } from '../components/dashboard/EventTrendChart';
import { SeverityDonut } from '../components/dashboard/SeverityDonut';
import { TopActorsTable } from '../components/dashboard/TopActorsTable';
import { QuotaUsageBar } from '../components/dashboard/QuotaUsageBar';
import { ActivityHeatmap } from '../components/dashboard/ActivityHeatmap';

// Error boundary — prevents one broken widget from blanking the whole page
class DashboardErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean; error?: string }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }
  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error: error.message };
  }
  render() {
    if (this.state.hasError) {
      return (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <strong>Widget error:</strong> {this.state.error}
        </div>
      );
    }
    return this.props.children;
  }
}

export function DashboardPage() {
  const { stats, trends } = useDashboardStats();

  // Safe data accessors — never let undefined bubble into child components
  const statsData = stats.data ?? null;
  const trendPoints = trends.data?.dataPoints ?? [];

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-sm text-gray-500 mt-0.5">Real-time audit trail overview</p>
        </div>
      </div>

      {/* Quota warning */}
      {statsData && (statsData.quotaUsedPercent ?? 0) > 80 && (
        <div className="flex items-center gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3">
          <AlertTriangle className="h-4 w-4 text-amber-600 flex-shrink-0" />
          <span className="text-sm text-amber-800">
            You've used <strong>{(statsData.quotaUsedPercent ?? 0).toFixed(0)}%</strong> of your monthly event quota.{' '}
            <a href="/settings/billing" className="font-semibold underline">Upgrade now →</a>
          </span>
        </div>
      )}

      {/* API error banner */}
      {stats.isError && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          Failed to load dashboard stats. Please refresh or log in again.
        </div>
      )}

      {/* Stat cards */}
      <DashboardErrorBoundary>
        <StatsCards stats={statsData ?? undefined} isLoading={stats.isLoading} />
      </DashboardErrorBoundary>

      {/* Charts row */}
      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-12 lg:col-span-8">
          <DashboardErrorBoundary>
            <EventTrendChart data={trendPoints} />
          </DashboardErrorBoundary>
        </div>
        <div className="col-span-12 lg:col-span-4">
          <DashboardErrorBoundary>
            <SeverityDonut stats={statsData ?? undefined} />
          </DashboardErrorBoundary>
        </div>
      </div>

      {/* Bottom row */}
      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-12 lg:col-span-5">
          <DashboardErrorBoundary>
            <TopActorsTable actors={statsData?.topActors ?? []} />
          </DashboardErrorBoundary>
        </div>
        <div className="col-span-12 lg:col-span-3">
          <DashboardErrorBoundary>
            <QuotaUsageBar
              used={statsData?.totalEventsThisMonth ?? 0}
              limit={statsData?.monthlyQuota ?? 10000}
            />
          </DashboardErrorBoundary>
        </div>
        <div className="col-span-12 lg:col-span-4">
          <DashboardErrorBoundary>
            <ActivityHeatmap data={trendPoints} />
          </DashboardErrorBoundary>
        </div>
      </div>
    </div>
  );
}
