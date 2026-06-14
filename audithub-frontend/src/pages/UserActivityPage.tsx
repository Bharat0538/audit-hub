import { useParams } from 'react-router-dom';
import { useState, useMemo } from 'react';
import { subDays } from 'date-fns';
import { useUserActivity } from '../hooks/useAuditEvents';
import { AuditEventTable } from '../components/audit/AuditEventTable';
import { AuditEventDetail } from '../components/audit/AuditEventDetail';
import { TableSkeleton } from '../components/ui/Skeleton';
import type { AuditEvent } from '../types';

export function UserActivityPage() {
  const { userId } = useParams<{ userId: string }>();
  const [selected, setSelected] = useState<AuditEvent | null>(null);

  // Stabilize the time range — compute ONCE on mount so the query key
  // never changes on re-renders (avoids infinite refetch loop).
  const timeRange = useMemo(() => ({
    startTime: subDays(new Date(), 30).toISOString(),
    endTime:   new Date().toISOString(),
  }), []); // empty deps → computed once when the component mounts

  const { data, isLoading } = useUserActivity(userId!, timeRange);

  return (
    <div className="flex h-full overflow-hidden">
      <div className={`flex flex-col flex-1 overflow-hidden ${selected ? 'w-1/2' : 'w-full'}`}>
        <div className="border-b border-gray-200 bg-white px-6 py-4">
          <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">User Activity</span>
          <h1 className="text-lg font-bold text-gray-900 mt-0.5 font-mono">{userId}</h1>
          <p className="text-xs text-gray-500 mt-0.5">Last 30 days · {data?.content.length ?? 0} events</p>
        </div>
        <div className="flex-1 overflow-auto">
          {isLoading ? <TableSkeleton /> : (
            <AuditEventTable events={data?.content ?? []} selectedEventId={selected?.eventId} onEventClick={setSelected} />
          )}
        </div>
      </div>
      {selected && (
        <div className="w-1/2 border-l border-gray-200 overflow-hidden">
          <AuditEventDetail event={selected} onClose={() => setSelected(null)} />
        </div>
      )}
    </div>
  );
}
