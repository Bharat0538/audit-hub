import { Table, TableHeader, TableBody, Th, Td } from '../ui/Table';
import { SeverityBadge, OutcomeBadge } from '../ui/Badge';
import type { AuditEvent } from '../../types';
import { formatIST, formatRelative } from '../../utils/date.utils';
import { cn } from '../../utils/formatting.utils';
import { ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';

interface AuditEventTableProps {
  events:          AuditEvent[];
  selectedEventId?:string;
  onEventClick:    (event: AuditEvent) => void;
}

export function AuditEventTable({ events, selectedEventId, onEventClick }: AuditEventTableProps) {
  return (
    <Table>
      <TableHeader>
        <tr>
          <Th>Time</Th>
          <Th>Actor</Th>
          <Th>Action</Th>
          <Th>Resource</Th>
          <Th>Severity</Th>
          <Th>Outcome</Th>
          <Th />
        </tr>
      </TableHeader>
      <TableBody>
        {events.map((event) => (
          <AuditEventRow
            key={event.eventId}
            event={event}
            isSelected={event.eventId === selectedEventId}
            onClick={() => onEventClick(event)}
          />
        ))}
      </TableBody>
    </Table>
  );
}

function AuditEventRow({ event, isSelected, onClick }: {
  event: AuditEvent; isSelected: boolean; onClick: () => void;
}) {
  return (
    <tr
      onClick={onClick}
      className={cn(
        'cursor-pointer transition-colors border-b border-gray-100',
        isSelected ? 'bg-blue-50/80' : 'hover:bg-gray-50'
      )}
    >
      <Td className="whitespace-nowrap">
        <div>
          <p className="font-medium text-gray-800 text-xs">{formatIST(event.eventTime)}</p>
          <p className="text-2xs text-gray-400 mt-0.5">{formatRelative(event.eventTime)}</p>
        </div>
      </Td>
      <Td>
        <div>
          <p className="font-medium text-gray-800 text-xs">{event.actor.userName ?? event.actor.userId}</p>
          <p className="text-2xs text-gray-400 mt-0.5">{event.actor.userEmail}</p>
        </div>
      </Td>
      <Td>
        <div>
          <p className="font-medium text-gray-800 text-xs font-mono">{event.action.name}</p>
          <p className="text-2xs text-gray-400 mt-0.5">{event.action.type}</p>
        </div>
      </Td>
      <Td>
        <div>
          <Link
            to={`/entities/${event.resource.type}/${event.resource.id}`}
            onClick={(e) => e.stopPropagation()}
            className="text-xs font-medium text-blue-600 hover:text-blue-700 flex items-center gap-1"
          >
            {event.resource.type}
            <ExternalLink className="h-2.5 w-2.5" />
          </Link>
          <p className="text-2xs text-gray-400 font-mono mt-0.5">{event.resource.id}</p>
        </div>
      </Td>
      <Td><SeverityBadge severity={event.severity} /></Td>
      <Td><OutcomeBadge outcome={event.outcome} /></Td>
      <Td>
        {event.changes && event.changes.length > 0 && (
          <span className="text-2xs text-gray-400">{event.changes.length} change{event.changes.length !== 1 ? 's' : ''}</span>
        )}
      </Td>
    </tr>
  );
}
