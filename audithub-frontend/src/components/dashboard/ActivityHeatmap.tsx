import type { TrendPoint } from '../../types';
import { parseISO, format } from 'date-fns';
import { cn } from '../../utils/formatting.utils';

export function ActivityHeatmap({ data }: { data: TrendPoint[] }) {
  const max = Math.max(...data.map((d) => d.count), 1);

  const getIntensity = (count: number) => {
    const ratio = count / max;
    if (count === 0)     return 'bg-gray-100';
    if (ratio < 0.25)    return 'bg-blue-100';
    if (ratio < 0.5)     return 'bg-blue-300';
    if (ratio < 0.75)    return 'bg-blue-500';
    return 'bg-blue-700';
  };

  return (
    <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-card h-full flex flex-col justify-between">
      <div>
        <h3 className="text-sm font-semibold text-gray-900 mb-4">Activity Heatmap</h3>
        <div className="flex flex-wrap gap-1">
          {data.map((point) => (
            <div
              key={point.date}
              title={`${format(parseISO(point.date), 'dd MMM')}: ${point.count} events`}
              className={cn('h-4 w-4 rounded cursor-default transition-colors', getIntensity(point.count))}
            />
          ))}
        </div>
      </div>
      <div className="flex items-center gap-1 mt-3">
        <span className="text-2xs text-gray-400 mr-1">Less</span>
        {['bg-gray-100','bg-blue-100','bg-blue-300','bg-blue-500','bg-blue-700'].map((c) => (
          <div key={c} className={cn('h-3 w-3 rounded', c)} />
        ))}
        <span className="text-2xs text-gray-400 ml-1">More</span>
      </div>
    </div>
  );
}
