import { useQuery } from '@tanstack/react-query';
import { organizationApi } from '../../api/auth.api';
import { Button } from '../../components/ui/Button';
import { Skeleton } from '../../components/ui/Skeleton';
import { formatNumber, formatBytes } from '../../utils/formatting.utils';
import { ArrowUpRight } from 'lucide-react';

const PLANS = [
  { name: 'Free',         price: '₹0',     events: '10K', retention: '7 days',   apps: 1 },
  { name: 'Starter',      price: '₹999',   events: '1M',  retention: '90 days',  apps: 5 },
  { name: 'Professional', price: '₹4,999', events: '10M', retention: '1 year',   apps: 20 },
  { name: 'Enterprise',   price: 'Custom', events: '∞',   retention: '7 years',  apps: '∞' },
];

export function BillingSettings() {
  const { data: usage, isLoading } = useQuery({ queryKey: ['org-usage'], queryFn: organizationApi.getUsage });
  const { data: org }              = useQuery({ queryKey: ['org-me'],    queryFn: organizationApi.getCurrent });

  return (
    <div className="p-6 max-w-3xl">
      <h1 className="text-xl font-bold text-gray-900 mb-6">Billing & Usage</h1>

      {/* Current usage */}
      <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-card mb-6">
        <h2 className="text-sm font-semibold text-gray-900 mb-4">Current Usage</h2>
        {isLoading ? <Skeleton className="h-20 w-full" /> : (
          <div className="space-y-4">
            <div>
              <div className="flex justify-between text-xs text-gray-500 mb-1.5">
                <span>Events this month</span>
                <span className="font-medium text-gray-700">
                  {formatNumber(usage?.eventsUsed ?? 0)} / {formatNumber(usage?.eventsLimit ?? 0)}
                </span>
              </div>
              <div className="h-2 w-full rounded-full bg-gray-100 overflow-hidden">
                <div className="h-full rounded-full bg-blue-500 transition-all"
                  style={{ width: `${Math.min(usage?.usagePercent ?? 0, 100)}%` }} />
              </div>
            </div>
            {usage?.storageUsedBytes !== undefined && (
              <div className="flex justify-between text-sm">
                <span className="text-gray-500">Storage used</span>
                <span className="font-medium text-gray-700">{formatBytes(usage.storageUsedBytes)}</span>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Plans */}
      <h2 className="text-sm font-semibold text-gray-900 mb-3">Choose a Plan</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {PLANS.map((plan) => {
          const isCurrent = org?.plan === plan.name.toUpperCase();
          return (
            <div key={plan.name} className={`rounded-xl border p-4 ${isCurrent ? 'border-blue-500 bg-blue-50' : 'border-gray-100 bg-white shadow-card'}`}>
              <div className="flex items-center justify-between mb-3">
                <p className="font-bold text-gray-900">{plan.name}</p>
                {isCurrent && <span className="text-2xs font-semibold text-blue-600 bg-blue-100 rounded-full px-2 py-0.5">Current</span>}
              </div>
              <p className="text-2xl font-bold text-gray-900 mb-3">{plan.price}<span className="text-sm font-normal text-gray-500">/mo</span></p>
              <div className="space-y-1 text-xs text-gray-600 mb-4">
                <p>✓ {plan.events} events/month</p>
                <p>✓ {plan.retention} retention</p>
                <p>✓ {plan.apps} application{plan.apps !== 1 ? 's' : ''}</p>
              </div>
              {!isCurrent && (
                <Button variant={plan.name === 'Enterprise' ? 'secondary' : 'primary'} size="sm" className="w-full"
                  icon={plan.name === 'Enterprise' ? undefined : <ArrowUpRight className="h-3.5 w-3.5" />}>
                  {plan.name === 'Enterprise' ? 'Contact Sales' : 'Upgrade'}
                </Button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
