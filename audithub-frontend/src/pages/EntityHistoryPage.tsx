import { useParams } from 'react-router-dom';
import { useEntityHistory } from '../hooks/useAuditEvents';
import { EntityTimeline } from '../components/audit/EntityTimeline';
import { Skeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { Clock } from 'lucide-react';

export function EntityHistoryPage() {
  const { resourceType, resourceId } = useParams<{ resourceType: string; resourceId: string }>();
  const { data, isLoading } = useEntityHistory(resourceType!, resourceId!);

  return (
    <div className="max-w-3xl mx-auto p-6">
      <div className="mb-6">
        <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">{resourceType}</span>
        <h1 className="text-xl font-bold text-gray-900 mt-1 font-mono">{resourceId}</h1>
        <p className="text-sm text-gray-500 mt-0.5">Complete audit history for this entity</p>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-20 w-full rounded-xl" />)}
        </div>
      ) : !data?.content.length ? (
        <EmptyState
          icon={<Clock className="h-8 w-8" />}
          title="No history found"
          description="No audit events have been recorded for this entity."
        />
      ) : (
        <EntityTimeline events={data.content} />
      )}
    </div>
  );
}
