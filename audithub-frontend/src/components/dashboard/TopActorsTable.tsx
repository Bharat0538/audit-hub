import type { TopActor } from '../../types';
import { Link } from 'react-router-dom';
import { formatNumber } from '../../utils/formatting.utils';

export function TopActorsTable({ actors }: { actors: TopActor[] }) {
  return (
    <div className="rounded-xl border border-gray-100 bg-white shadow-card h-full">
      <div className="border-b border-gray-100 px-5 py-3">
        <h3 className="text-sm font-semibold text-gray-900">Top Active Users</h3>
        <p className="text-xs text-gray-500 mt-0.5">This month</p>
      </div>
      <div className="divide-y divide-gray-50 max-h-[300px] overflow-y-auto">
        {actors.map((actor, i) => (
          <div key={actor.userId} className="flex items-center gap-3 px-5 py-3">
            <span className="w-5 text-xs font-bold text-gray-400 text-center">{i + 1}</span>
            <div className="h-7 w-7 flex-shrink-0 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center">
              <span className="text-xs font-bold text-white">
                {actor.userName ? actor.userName.charAt(0).toUpperCase() : actor.userId.charAt(0).toUpperCase()}
              </span>
            </div>
            <div className="flex-1 min-w-0">
              <Link to={`/users/${actor.userId}/activity`}
                className="text-sm font-medium text-gray-900 hover:text-blue-600 transition-colors truncate block">
                {actor.userName || actor.userId}
              </Link>
              <p className="text-xs text-gray-400 font-mono truncate">{actor.userId}</p>
            </div>
            <div className="text-right">
              <p className="text-sm font-semibold text-gray-900">{formatNumber(actor.eventCount)}</p>
              <p className="text-2xs text-gray-400">events</p>
            </div>
          </div>
        ))}
        {actors.length === 0 && (
          <p className="text-sm text-gray-400 text-center py-8">No data yet</p>
        )}
      </div>
    </div>
  );
}
