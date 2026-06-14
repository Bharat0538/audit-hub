import { useNavigate } from 'react-router-dom';
import { formatNumber } from '../../utils/formatting.utils';
import { cn } from '../../utils/formatting.utils';

interface QuotaUsageBarProps {
  used:  number;
  limit: number;
}

export function QuotaUsageBar({ used, limit }: QuotaUsageBarProps) {
  const pct = Math.min((used / limit) * 100, 100);
  const navigate = useNavigate();

  const color = pct >= 90 ? 'bg-red-500' : pct >= 80 ? 'bg-amber-500' : 'bg-blue-505'; // wait bg-blue-500 or tailwind code
  const barColor = pct >= 90 ? 'bg-red-500' : pct >= 80 ? 'bg-amber-500' : 'bg-blue-500';
  const label = pct >= 90 ? 'text-red-600' : pct >= 80 ? 'text-amber-600' : 'text-blue-600';

  return (
    <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-card h-full flex flex-col justify-between">
      <div>
        <div className="flex items-center justify-between mb-3">
          <div>
            <h3 className="text-sm font-semibold text-gray-900">Monthly Quota</h3>
            <p className="text-xs text-gray-500 mt-0.5">Resets on 1st of next month</p>
          </div>
          <span className={cn('text-xl font-bold', label)}>{pct.toFixed(1)}%</span>
        </div>
        <div className="h-2.5 w-full rounded-full bg-gray-100 overflow-hidden">
          <div className={cn('h-full rounded-full transition-all duration-500', barColor)} style={{ width: `${pct}%` }} />
        </div>
        <div className="flex justify-between mt-2">
          <span className="text-xs text-gray-500">{formatNumber(used)} used</span>
          <span className="text-xs text-gray-500">{formatNumber(limit)} limit</span>
        </div>
      </div>
      {pct >= 80 && (
        <button onClick={() => navigate('/settings/billing')}
          className="mt-3 w-full rounded-lg border border-amber-200 bg-amber-50 py-2 text-xs font-medium text-amber-700 hover:bg-amber-100 transition-colors cursor-pointer">
          Upgrade Plan →
        </button>
      )}
    </div>
  );
}
