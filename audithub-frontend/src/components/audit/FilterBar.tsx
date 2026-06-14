import { useState } from 'react';
import { SlidersHorizontal, X, Search } from 'lucide-react';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { DateRangePicker } from '../ui/DateRangePicker';
import type { EventFilters, ActionType, Severity, Outcome } from '../../types';
import { cn } from '../../utils/formatting.utils';

interface FilterBarProps {
  filters:  EventFilters;
  onChange: (f: EventFilters) => void;
}

const ACTION_TYPES: ActionType[] = ['CREATE','UPDATE','DELETE','READ','LOGIN','LOGOUT','EXPORT','APPROVE','REJECT','TRANSFER'];
const SEVERITIES:   Severity[]   = ['LOW','MEDIUM','HIGH','CRITICAL'];
const OUTCOMES:     Outcome[]    = ['SUCCESS','FAILURE','PARTIAL'];

function activeFilterCount(f: EventFilters): number {
  let count = 0;
  if (f.actionTypes?.length)  count++;
  if (f.severities?.length)   count++;
  if (f.outcomes?.length)     count++;
  if (f.resourceType)         count++;
  if (f.actorUserId)          count++;
  if (f.tags?.length)         count++;
  return count;
}

export function FilterBar({ filters, onChange }: FilterBarProps) {
  const [expanded, setExpanded] = useState(false);
  const active = activeFilterCount(filters);

  const toggle = <T extends string>(arr: T[] | undefined, val: T): T[] => {
    const a = arr ?? [];
    return a.includes(val) ? a.filter((v) => v !== val) : [...a, val];
  };

  const clear = () =>
    onChange({ startTime: filters.startTime, endTime: filters.endTime });

  return (
    <div className="space-y-3">
      {/* Primary row */}
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex-1 min-w-64">
          <Input
            placeholder="Search events…"
            value={filters.query ?? ''}
            onChange={(e) => onChange({ ...filters, query: e.target.value })}
            icon={<Search className="h-4 w-4" />}
          />
        </div>
        <DateRangePicker
          startTime={filters.startTime}
          endTime={filters.endTime}
          onChange={(s, e) => onChange({ ...filters, startTime: s, endTime: e })}
        />
        <Button
          variant="secondary" size="sm"
          icon={<SlidersHorizontal className="h-4 w-4" />}
          onClick={() => setExpanded(!expanded)}
        >
          Filters
          {active > 0 && (
            <span className="ml-1 flex h-4 w-4 items-center justify-center rounded-full bg-blue-600 text-2xs font-bold text-white">
              {active}
            </span>
          )}
        </Button>
        {active > 0 && (
          <Button variant="ghost" size="sm" icon={<X className="h-3 w-3" />} onClick={clear}>
            Clear
          </Button>
        )}
      </div>

      {/* Expanded filters */}
      {expanded && (
        <div className="rounded-lg border border-gray-200 bg-gray-50 p-3 space-y-3 animate-slide-in-up">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {/* Action types */}
            <div>
              <p className="text-xs font-medium text-gray-500 mb-2">Action Type</p>
              <div className="flex flex-wrap gap-1">
                {ACTION_TYPES.map((t) => (
                  <button key={t} onClick={() => onChange({ ...filters, actionTypes: toggle(filters.actionTypes, t) })}
                    className={cn('rounded px-2 py-0.5 text-xs font-medium border transition-colors cursor-pointer',
                      filters.actionTypes?.includes(t)
                        ? 'border-blue-500 bg-blue-500 text-white'
                        : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300 hover:bg-gray-50'
                    )}>
                    {t}
                  </button>
                ))}
              </div>
            </div>

            {/* Severity */}
            <div>
              <p className="text-xs font-medium text-gray-500 mb-2">Severity</p>
              <div className="flex flex-wrap gap-1">
                {SEVERITIES.map((s) => (
                  <button key={s} onClick={() => onChange({ ...filters, severities: toggle(filters.severities, s) })}
                    className={cn('rounded px-2 py-0.5 text-xs font-medium border transition-colors cursor-pointer',
                      filters.severities?.includes(s)
                        ? 'border-blue-500 bg-blue-500 text-white'
                        : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                    )}>
                    {s}
                  </button>
                ))}
              </div>
            </div>

            {/* Outcome */}
            <div>
              <p className="text-xs font-medium text-gray-500 mb-2">Outcome</p>
              <div className="flex flex-wrap gap-1">
                {OUTCOMES.map((o) => (
                  <button key={o} onClick={() => onChange({ ...filters, outcomes: toggle(filters.outcomes, o) })}
                    className={cn('rounded px-2 py-0.5 text-xs font-medium border transition-colors cursor-pointer',
                      filters.outcomes?.includes(o)
                        ? 'border-blue-500 bg-blue-500 text-white'
                        : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                    )}>
                    {o}
                  </button>
                ))}
              </div>
            </div>

            {/* Resource type */}
            <div>
              <p className="text-xs font-medium text-gray-500 mb-2">Resource Type</p>
              <Input
                placeholder="e.g. LoanApplication"
                value={filters.resourceType ?? ''}
                onChange={(e) => onChange({ ...filters, resourceType: e.target.value || undefined })}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
