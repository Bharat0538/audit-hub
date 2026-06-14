import { useQuery } from '@tanstack/react-query';
import { organizationApi } from '../../api/auth.api';
import { Badge } from '../../components/ui/Badge';
import { Database } from 'lucide-react';

export function RetentionSettings() {
  const { data: org } = useQuery({ queryKey: ['org-me'], queryFn: organizationApi.getCurrent });

  const retentionDays  = org?.retentionDays ?? 7;
  const retentionLabel = retentionDays >= 365 ? `${Math.floor(retentionDays / 365)} year${Math.floor(retentionDays/365)>1?'s':''}` : `${retentionDays} days`;

  return (
    <div className="p-6 max-w-2xl">
      <h1 className="text-xl font-bold text-gray-900 mb-6">Data Retention</h1>

      <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-card space-y-5">
        <div className="flex items-center gap-4 p-4 rounded-lg bg-blue-50">
          <Database className="h-8 w-8 text-blue-600 flex-shrink-0" />
          <div>
            <p className="text-sm font-semibold text-blue-900">Current Retention Period</p>
            <p className="text-2xl font-bold text-blue-700 mt-0.5">{retentionLabel}</p>
            <p className="text-xs text-blue-600 mt-0.5">Events older than {retentionLabel} are automatically deleted</p>
          </div>
        </div>

        <div className="space-y-3 text-sm text-gray-700">
          <p className="font-medium text-gray-900">Retention by plan:</p>
          {[
            ['FREE',         '7 days',  'gray'],
            ['STARTER',      '90 days', 'blue'],
            ['PROFESSIONAL', '1 year',  'blue'],
            ['ENTERPRISE',   '7 years', 'purple'],
          ].map(([plan, days, color]) => (
            <div key={plan} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
              <Badge variant={color as 'gray' | 'blue' | 'purple'}>{plan}</Badge>
              <span className="text-sm font-medium text-gray-700">{days}</span>
            </div>
          ))}
        </div>

        <p className="text-xs text-gray-400">
          Upgrade your plan to increase retention. For custom periods or DPDP/RBI compliance requirements, contact Enterprise sales.
        </p>
      </div>
    </div>
  );
}
