import type { AuditEvent } from '../../types';
import { SeverityBadge, OutcomeBadge } from '../ui/Badge';
import { DiffViewer } from './DiffViewer';
import { formatIST } from '../../utils/date.utils';
import { useState } from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '../../utils/formatting.utils';

interface EntityTimelineProps {
  events: AuditEvent[];
}

export function EntityTimeline({ events }: EntityTimelineProps) {
  return (
    <div className="relative">
      {/* Vertical line */}
      <div className="absolute left-4 top-4 bottom-4 w-px bg-gray-200" />
      <div className="space-y-4">
        {events.map((event, i) => (
          <TimelineEvent key={event.eventId} event={event} isLast={i === events.length - 1} />
        ))}
      </div>
    </div>
  );
}

function TimelineEvent({ event, isLast }: { event: AuditEvent; isLast: boolean }) {
  const [open, setOpen] = useState(false);
  const hasChanges = event.changes && event.changes.length > 0;

  return (
    <div className="flex gap-4 pl-0">
      {/* Dot */}
      <div className={cn(
        'relative z-10 flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full border-2 border-white',
        event.severity === 'CRITICAL' ? 'bg-purple-100 ring-2 ring-purple-300' :
        event.severity === 'HIGH'     ? 'bg-red-100' :
        event.severity === 'MEDIUM'   ? 'bg-amber-100' : 'bg-green-100'
      )}>
        <span className="text-xs font-bold text-gray-600">
          {event.action.type.charAt(0)}
        </span>
      </div>

      {/* Content */}
      <div className="flex-1 min-w-0 pb-4">
        <div
          className={cn(
            'rounded-lg border border-gray-100 bg-white shadow-card',
            hasChanges && 'cursor-pointer hover:border-gray-200 transition-colors'
          )}
          onClick={() => hasChanges && setOpen(!open)}
        >
          <div className="flex items-center justify-between p-3">
            <div className="min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <span className="text-sm font-medium text-gray-900">{event.action.name}</span>
                <SeverityBadge severity={event.severity} />
                <OutcomeBadge outcome={event.outcome} />
              </div>
              <div className="text-xs text-gray-500">
                <span className="font-medium text-gray-700">{event.actor.userName ?? event.actor.userId}</span>
                {' · '}
                {formatIST(event.eventTime)}
              </div>
            </div>
            {hasChanges && (
              <ChevronDown className={cn('h-4 w-4 text-gray-400 flex-shrink-0 transition-transform', open && 'rotate-180')} />
            )}
          </div>

          {open && hasChanges && (
            <div className="border-t border-gray-100 p-3">
              <DiffViewer changes={event.changes} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
