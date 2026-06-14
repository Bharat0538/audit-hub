import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import type { TrendPoint } from '../../types';
import { formatShort } from '../../utils/date.utils';

interface EventTrendChartProps {
  data: TrendPoint[];
}

export function EventTrendChart({ data }: EventTrendChartProps) {
  const formatted = data.map((d) => ({
    ...d,
    date: formatShort(d.date + 'T00:00:00Z'),
  }));

  return (
    <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-card">
      <div className="mb-4">
        <h3 className="text-sm font-semibold text-gray-900">Event Volume</h3>
        <p className="text-xs text-gray-500 mt-0.5">Last 30 days</p>
      </div>
      <ResponsiveContainer width="100%" height={200}>
        <AreaChart data={formatted} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
          <defs>
            <linearGradient id="colorTotal" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%"  stopColor="#3b82f6" stopOpacity={0.15} />
              <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
            </linearGradient>
            <linearGradient id="colorCritical" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%"  stopColor="#7c3aed" stopOpacity={0.15} />
              <stop offset="95%" stopColor="#7c3aed" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
          <XAxis dataKey="date" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} />
          <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} />
          <Tooltip
            contentStyle={{ background: '#1e293b', border: 'none', borderRadius: 8, fontSize: 12, color: '#f1f5f9' }}
            cursor={{ stroke: '#3b82f6', strokeWidth: 1, strokeDasharray: '4 4' }}
          />
          <Legend wrapperStyle={{ fontSize: 11, paddingTop: 8 }} />
          <Area type="monotone" dataKey="count" name="Total" stroke="#3b82f6" fill="url(#colorTotal)" strokeWidth={2} dot={false} />
          <Area type="monotone" dataKey="criticalCount" name="Critical" stroke="#7c3aed" fill="url(#colorCritical)" strokeWidth={1.5} dot={false} />
          <Area type="monotone" dataKey="failureCount" name="Failures" stroke="#ef4444" fill="none" strokeWidth={1.5} strokeDasharray="4 4" dot={false} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
