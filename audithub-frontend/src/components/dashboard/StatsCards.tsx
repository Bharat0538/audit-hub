import { TrendingUp, AlertTriangle, XCircle, Layers } from 'lucide-react';
import type { DashboardStats } from '../../types';
import { formatNumber } from '../../utils/formatting.utils';
import { SkeletonCard } from '../ui/Skeleton';

interface StatsCardsProps {
  stats?:     DashboardStats;
  isLoading?: boolean;
}

export function StatsCards({ stats, isLoading }: StatsCardsProps) {
  if (isLoading) return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      {Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)}
    </div>
  );
  if (!stats) return null;

  const cards = [
    {
      label: 'Events Today',
      value: formatNumber(stats.totalEventsToday),
      sub:   `${formatNumber(stats.totalEventsThisMonth)} this month`,
      icon:  <TrendingUp className="h-5 w-5 text-blue-600" />,
      bg:    'bg-blue-50',
    },
    {
      label: 'Critical Events',
      value: formatNumber(stats.criticalEventsToday),
      sub:   'today',
      icon:  <AlertTriangle className="h-5 w-5 text-purple-600" />,
      bg:    'bg-purple-50',
      warn:  stats.criticalEventsToday > 0,
    },
    {
      label: 'Failed Events',
      value: formatNumber(stats.failedEventsToday),
      sub:   'today',
      icon:  <XCircle className="h-5 w-5 text-red-500" />,
      bg:    'bg-red-50',
      warn:  stats.failedEventsToday > 5,
    },
    {
      label: 'Quota Used',
      value: `${stats.quotaUsedPercent.toFixed(1)}%`,
      sub:   `${formatNumber(stats.totalEventsThisMonth)} / ${formatNumber(stats.monthlyQuota)}`,
      icon:  <Layers className="h-5 w-5 text-emerald-600" />,
      bg:    'bg-emerald-50',
      warn:  stats.quotaUsedPercent > 80,
    },
  ];

  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      {cards.map((card) => (
        <div key={card.label}
          className="rounded-xl border border-gray-100 bg-white p-5 shadow-card hover:shadow-card-hover transition-shadow">
          <div className="flex items-center justify-between mb-3">
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{card.label}</p>
            <div className={`rounded-lg p-1.5 ${card.bg}`}>{card.icon}</div>
          </div>
          <p className={`text-2xl font-bold ${card.warn ? 'text-red-600' : 'text-gray-900'}`}>{card.value}</p>
          <p className="text-xs text-gray-500 mt-1">{card.sub}</p>
        </div>
      ))}
    </div>
  );
}
