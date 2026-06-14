import { useState, useCallback } from 'react';
import { subDays } from 'date-fns';
import { Download, RefreshCw, ClipboardList } from 'lucide-react';
import { FilterBar } from '../components/audit/FilterBar';
import { AuditEventTable } from '../components/audit/AuditEventTable';
import { AuditEventDetail } from '../components/audit/AuditEventDetail';
import { Pagination } from '../components/ui/Pagination';
import { Button } from '../components/ui/Button';
import { TableSkeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { useAuditEvents } from '../hooks/useAuditEvents';
import type { EventFilters, AuditEvent } from '../types';
import { cn } from '../utils/formatting.utils';
import { startOfDayISO, endOfDayISO } from '../utils/date.utils';

const DEFAULT_FILTERS: EventFilters = {
  startTime: startOfDayISO(subDays(new Date(), 7)),
  endTime:   endOfDayISO(new Date()),
};

export function EventsPage() {
  const [filters, setFilters]           = useState<EventFilters>(DEFAULT_FILTERS);
  const [pageToken, setPageToken]       = useState<string | undefined>();
  const [pageHistory, setPageHistory]   = useState<string[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<AuditEvent | null>(null);

  const { data, isLoading, isFetching, refetch } = useAuditEvents(filters, { pageToken });

  const handleFiltersChange = useCallback((f: EventFilters) => {
    setFilters(f);
    setPageToken(undefined);
    setPageHistory([]);
    setSelectedEvent(null);
  }, []);

  const goNext = () => {
    if (data?.pageToken) {
      setPageHistory((h) => [...h, pageToken ?? '']);
      setPageToken(data.pageToken);
    }
  };

  const goPrev = () => {
    const history = [...pageHistory];
    const prev = history.pop();
    setPageHistory(history);
    setPageToken(prev ?? undefined);
  };

  return (
    <div className="flex h-full overflow-hidden">
      {/* Main panel */}
      <div className={cn('flex flex-col flex-1 overflow-hidden', selectedEvent ? 'w-1/2' : 'w-full')}>
        {/* Filter bar */}
        <div className="border-b border-gray-200 bg-white p-4 sticky top-0 z-10">
          <FilterBar filters={filters} onChange={handleFiltersChange} />
        </div>

        {/* Results meta */}
        <div className="flex items-center justify-between border-b border-gray-100 bg-gray-50 px-4 py-2">
          <span className="text-xs text-gray-500">
            {isFetching && !isLoading ? (
              <span className="flex items-center gap-1"><RefreshCw className="h-3 w-3 animate-spin" /> Refreshing…</span>
            ) : (
              <>~<span className="font-medium text-gray-700">{data?.totalEstimate?.toLocaleString('en-IN') ?? 0}</span> events</>
            )}
          </span>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="xs" icon={<RefreshCw className="h-3 w-3" />} onClick={() => refetch()}>Refresh</Button>
            <Button variant="ghost" size="xs" icon={<Download className="h-3 w-3" />}>Export CSV</Button>
          </div>
        </div>

        {/* Table */}
        <div className="flex-1 overflow-auto">
          {isLoading ? <TableSkeleton rows={12} cols={7} /> :
           !data?.content.length ? (
             <EmptyState
               icon={<ClipboardList className="h-8 w-8" />}
               title="No events found"
               description="Try adjusting your filters or time range."
             />
           ) : (
             <AuditEventTable
               events={data.content}
               selectedEventId={selectedEvent?.eventId}
               onEventClick={setSelectedEvent}
             />
           )}
        </div>

        {/* Pagination */}
        {data && (
          <Pagination
            hasMore={data.hasMore}
            pageToken={data.pageToken}
            canGoPrev={pageHistory.length > 0}
            onNext={goNext}
            onPrev={goPrev}
            totalEstimate={data.totalEstimate}
            pageSize={50}
            currentCount={data.content.length}
          />
        )}
      </div>

      {/* Detail panel */}
      {selectedEvent && (
        <div className="w-1/2 border-l border-gray-200 overflow-hidden flex-shrink-0">
          <AuditEventDetail event={selectedEvent} onClose={() => setSelectedEvent(null)} />
        </div>
      )}
    </div>
  );
}
