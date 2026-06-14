import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { useAuditEvent } from '../hooks/useAuditEvents';
import { AuditEventDetail } from '../components/audit/AuditEventDetail';
import { Skeleton } from '../components/ui/Skeleton';
import { Button } from '../components/ui/Button';

export function EventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const { data, isLoading } = useAuditEvent(
    eventId!,
    params.get('applicationId') ?? '',
    params.get('eventTime') ?? ''
  );

  if (isLoading) return (
    <div className="p-6 space-y-4">
      <Skeleton className="h-6 w-48" />
      <Skeleton className="h-96 w-full rounded-xl" />
    </div>
  );

  if (!data) return (
    <div className="p-6">
      <p className="text-sm text-gray-500">Event not found.</p>
    </div>
  );

  return (
    <div className="max-w-3xl mx-auto p-6">
      <Button variant="ghost" size="sm" icon={<ArrowLeft className="h-4 w-4" />}
        className="mb-4" onClick={() => navigate(-1)}>Back</Button>
      <div className="rounded-xl border border-gray-200 overflow-hidden">
        <AuditEventDetail event={data} onClose={() => navigate(-1)} />
      </div>
    </div>
  );
}
