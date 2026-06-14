import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import type { DashboardStats } from '../../types';

const COLORS = { Low: '#22c55e', Critical: '#7c3aed', Failures: '#ef4444' };

export function SeverityDonut({ stats }: { stats?: DashboardStats }) {
  if (!stats) return null;

  const data = [
    { name: 'Low',      value: Math.max(0, stats.totalEventsToday - stats.criticalEventsToday - stats.failedEventsToday) },
    { name: 'Critical', value: stats.criticalEventsToday },
    { name: 'Failures', value: stats.failedEventsToday },
  ].filter((d) => d.value > 0);

  return (
    <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-card h-full flex flex-col justify-between">
      <h3 className="text-sm font-semibold text-gray-900 mb-4">Today's Events</h3>
      <div className="flex-1 flex items-center justify-center">
        <ResponsiveContainer width="100%" height={180}>
          <PieChart>
            <Pie data={data} cx="50%" cy="50%" innerRadius={50} outerRadius={75}
              paddingAngle={3} dataKey="value">
              {data.map((entry) => (
                <Cell key={entry.name} fill={COLORS[entry.name as keyof typeof COLORS] ?? '#94a3b8'} />
              ))}
            </Pie>
            <Tooltip contentStyle={{ background: '#1e293b', border: 'none', borderRadius: 8, fontSize: 12, color: '#f1f5f9' }} />
            <Legend wrapperStyle={{ fontSize: 11 }} />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
