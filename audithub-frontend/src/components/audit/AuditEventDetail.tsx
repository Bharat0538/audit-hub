import { X, Copy, ExternalLink, Code } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { AuditEvent } from '../../types';
import { SeverityBadge, OutcomeBadge, Badge } from '../ui/Badge';
import { DiffViewer } from './DiffViewer';
import { formatIST } from '../../utils/date.utils';
import { copyToClipboard, truncate } from '../../utils/formatting.utils';
import toast from 'react-hot-toast';
import { useState } from 'react';

interface AuditEventDetailProps {
  event:   AuditEvent;
  onClose: () => void;
}

export function AuditEventDetail({ event, onClose }: AuditEventDetailProps) {
  const [showRaw, setShowRaw] = useState(false);

  const copy = () => {
    copyToClipboard(JSON.stringify(event, null, 2));
    toast.success('Copied to clipboard');
  };

  return (
    <div className="h-full flex flex-col bg-white animate-slide-in-right">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3 sticky top-0 bg-white z-10">
        <div className="flex items-center gap-2 min-w-0">
          <SeverityBadge severity={event.severity} />
          <span className="truncate text-sm font-medium text-gray-900">{event.action.name}</span>
        </div>
        <button onClick={onClose} className="ml-2 rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors flex-shrink-0 cursor-pointer">
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto p-4 space-y-5">

        {/* Meta */}
        <Section title="Event Details">
          <MetaRow label="Event ID">
            <code className="text-2xs font-mono text-gray-700 bg-gray-50 px-1.5 py-0.5 rounded">{truncate(event.eventId, 36)}</code>
          </MetaRow>
          <MetaRow label="Time">{formatIST(event.eventTime)} IST</MetaRow>
          <MetaRow label="Outcome"><OutcomeBadge outcome={event.outcome} /></MetaRow>
          <MetaRow label="Action Type"><Badge variant="gray">{event.action.type}</Badge></MetaRow>
          {event.correlationId && (
            <MetaRow label="Correlation">
              <code className="text-2xs font-mono text-gray-600">{event.correlationId}</code>
            </MetaRow>
          )}
        </Section>

        {/* Actor */}
        <Section title="Actor">
          <div className="flex items-center gap-3 mb-3">
            <div className="h-9 w-9 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
              <span className="text-sm font-bold text-blue-700">
                {(event.actor.userName ?? event.actor.userId).charAt(0).toUpperCase()}
              </span>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-900">{event.actor.userName ?? event.actor.userId}</p>
              <p className="text-xs text-gray-500">{event.actor.userEmail}</p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <SmallMetaRow label="User ID">{event.actor.userId}</SmallMetaRow>
            {event.actor.ipAddress && <SmallMetaRow label="IP">{event.actor.ipAddress}</SmallMetaRow>}
          </div>
          <Link
            to={`/users/${event.actor.userId}/activity`}
            className="mt-3 inline-flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700"
          >
            View all activity <ExternalLink className="h-3 w-3" />
          </Link>
        </Section>

        {/* Resource */}
        <Section title="Resource">
          <MetaRow label="Type"><Badge variant="blue">{event.resource.type}</Badge></MetaRow>
          <MetaRow label="ID"><code className="text-2xs font-mono text-gray-700">{event.resource.id}</code></MetaRow>
          {event.resource.name && <MetaRow label="Name">{event.resource.name}</MetaRow>}
          <Link
            to={`/entities/${event.resource.type}/${event.resource.id}`}
            className="mt-2 inline-flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700"
          >
            View entity history <ExternalLink className="h-3 w-3" />
          </Link>
        </Section>

        {/* Changes */}
        {event.changes && event.changes.length > 0 && (
          <Section title={`Changes (${event.changes.length})`}>
            <DiffViewer changes={event.changes} />
          </Section>
        )}

        {/* Metadata */}
        {event.metadata && Object.keys(event.metadata).length > 0 && (
          <Section title="Metadata">
            <div className="rounded-lg bg-gray-50 p-3 space-y-1.5">
              {Object.entries(event.metadata).map(([k, v]) => (
                <div key={k} className="flex justify-between text-xs">
                  <span className="font-mono text-gray-500">{k}</span>
                  <span className="text-gray-700 ml-4 text-right max-w-40 truncate">{v}</span>
                </div>
              ))}
            </div>
          </Section>
        )}

        {/* Tags */}
        {event.tags && event.tags.length > 0 && (
          <Section title="Tags">
            <div className="flex flex-wrap gap-1.5">
              {event.tags.map((tag) => (
                <Badge key={tag} variant="blue" size="sm">#{tag}</Badge>
              ))}
            </div>
          </Section>
        )}

        {/* Raw JSON */}
        {showRaw && (
          <Section title="Raw Payload">
            <pre className="text-2xs font-mono bg-gray-900 text-green-400 rounded-lg p-3 overflow-x-auto">
              {JSON.stringify(event, null, 2)}
            </pre>
          </Section>
        )}
      </div>

      {/* Footer */}
      <div className="border-t border-gray-100 px-4 py-3">
        <div className="flex gap-2">
          <button onClick={copy}
            className="flex-1 flex items-center justify-center gap-1.5 rounded-lg border border-gray-200 py-2 text-xs font-medium text-gray-600 hover:bg-gray-50 transition-colors cursor-pointer">
            <Copy className="h-3.5 w-3.5" /> Copy JSON
          </button>
          <button onClick={() => setShowRaw(!showRaw)}
            className="flex-1 flex items-center justify-center gap-1.5 rounded-lg border border-gray-200 py-2 text-xs font-medium text-gray-600 hover:bg-gray-50 transition-colors cursor-pointer">
            <Code className="h-3.5 w-3.5" /> {showRaw ? 'Hide' : 'Raw'}
          </button>
        </div>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h3 className="mb-2 text-xs font-semibold uppercase tracking-wider text-gray-400">{title}</h3>
      {children}
    </div>
  );
}

function MetaRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between py-1 text-xs">
      <span className="text-gray-500 flex-shrink-0 w-28">{label}</span>
      <span className="text-gray-900 text-right">{children}</span>
    </div>
  );
}

function SmallMetaRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="bg-gray-50 rounded p-2">
      <p className="text-2xs text-gray-400 mb-0.5">{label}</p>
      <p className="text-xs font-mono text-gray-700 truncate">{children as string}</p>
    </div>
  );
}
