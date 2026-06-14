
---

## 9. Kafka Topics & Event Schema

### Topic Configuration

```
Topic: audit.events.enriched
  Partitions:    32  (scale by org volume)
  Replication:   3
  Retention:     7 days (raw) — Cassandra is source of truth
  Cleanup:       delete
  Partition Key: organizationId (consistent ordering per tenant)
  Compression:   LZ4

Topic: audit.events.dlq
  Partitions:    8
  Replication:   3
  Retention:     30 days
  Alert:         PagerDuty on any message arrival

Topic: audit.events.replay
  Partitions:    16
  Replication:   3
  Retention:     24 hours
  Purpose:       Tenant-triggered replay events

Topic: audit.alerts
  Partitions:    8
  Replication:   3
  Retention:     7 days
  Consumer:      Notification service
```

### Kafka Message Envelope Schema

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "organizationId": "org-uuid",
  "applicationId": "app-uuid",
  "eventTime": "2025-06-13T10:30:00Z",
  "monthBucket": "2025-06",
  "actor": {
    "userId": "EMP-12345",
    "userEmail": "priya@hdfc.com",
    "userName": "Priya Nair",
    "ipAddress": "103.21.58.1",
    "userAgent": "Mozilla/5.0...",
    "sessionId": "sess_abc123"
  },
  "action": {
    "type": "UPDATE",
    "name": "loan.status.approved",
    "description": "Home loan approved by branch manager"
  },
  "resource": {
    "type": "LoanApplication",
    "id": "LOAN-2025-001234",
    "name": "Home Loan - Rahul Gupta",
    "path": "/api/v1/loans/LOAN-2025-001234"
  },
  "changes": [
    { "fieldName": "status",         "oldValue": "UNDER_REVIEW", "newValue": "APPROVED" },
    { "fieldName": "approvedAmount", "oldValue": null,           "newValue": "5000000" }
  ],
  "outcome": "SUCCESS",
  "severity": "HIGH",
  "correlationId": "req_789xyz",
  "metadata": { "branchCode": "MUM-001" },
  "tags": ["loan", "approval"],
  "geoCountry": "IN",
  "geoCity": "Mumbai",
  "ingestedAt": "2025-06-13T10:30:00.123Z",
  "rawPayload": "{...original...}"
}
```

### DLQ Message Schema

```json
{
  "originalEvent": { "...": "..." },
  "errorReason": "Cassandra write timeout after 3 retries",
  "errorTimestamp": "2025-06-13T10:30:05Z",
  "retryCount": 3,
  "serviceId": "storage-service-pod-2"
}
```

---

## 10. Redis Caching Strategy

```
Cache Layer 1: API Key Validation
  Key:    apikey:valid:{key_prefix}
  Value:  JSON(ApiKeyPrincipal)
  TTL:    5 minutes
  Reason: Avoid DB hit on every event ingestion

Cache Layer 2: Organization Quota Counter
  Key:    quota:monthly:{orgId}:{yearMonth}
  Value:  Long (event count)
  TTL:    35 days (covers full month + buffer)
  Ops:    INCR (atomic, Redis-native)

Cache Layer 3: Organization Config
  Key:    org:config:{orgId}
  Value:  JSON(Organization) — plan, retention, limits
  TTL:    10 minutes
  Evict:  On plan change / settings update

Cache Layer 4: Dashboard Stats
  Key:    dashboard:stats:{orgId}:{appId?}
  Value:  JSON(DashboardStats)
  TTL:    2 minutes
  Reason: Dashboard is read-heavy but can tolerate slight staleness

Cache Layer 5: Query Results (Simple Queries)
  Key:    query:{orgId}:{appId}:{startHour}:{endHour}:{pageSize}
  Value:  JSON(AuditEventPage)
  TTL:    5 minutes
  Evict:  Not needed — event data is immutable

Cache Layer 6: JWT Blacklist
  Key:    jwt:revoked:{jti}
  Value:  "1"
  TTL:    JWT expiry time
  Reason: Logout invalidation without DB hit

Cache Layer 7: Idempotency Keys
  Key:    idem:{orgId}:{eventId}
  Value:  "1"
  TTL:    24 hours
  Reason: Prevent duplicate event storage
```

```java
// Redis configuration
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withCacheConfiguration("org-config",
                        config.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("dashboard-stats",
                        config.entryTtl(Duration.ofMinutes(2)))
                .build();
    }
}
```

---

## 11. Frontend React Application Design

### Application Structure

```
audithub-frontend/
├── public/
│   └── index.html
├── src/
│   ├── api/                    # Axios instances + typed API calls
│   │   ├── client.ts           # Base axios with interceptors
│   │   ├── events.api.ts
│   │   ├── reports.api.ts
│   │   ├── organizations.api.ts
│   │   └── auth.api.ts
│   ├── components/
│   │   ├── ui/                 # Design system primitives (shadcn/ui base)
│   │   │   ├── Button.tsx
│   │   │   ├── Input.tsx
│   │   │   ├── Badge.tsx
│   │   │   ├── Table.tsx
│   │   │   ├── Modal.tsx
│   │   │   ├── Dropdown.tsx
│   │   │   ├── DateRangePicker.tsx
│   │   │   ├── Pagination.tsx
│   │   │   └── Skeleton.tsx
│   │   ├── audit/
│   │   │   ├── AuditEventTable.tsx
│   │   │   ├── AuditEventRow.tsx
│   │   │   ├── AuditEventDetail.tsx   # Side panel
│   │   │   ├── DiffViewer.tsx         # Old vs New value comparison
│   │   │   ├── EntityTimeline.tsx     # Entity history waterfall
│   │   │   └── FilterBar.tsx
│   │   ├── dashboard/
│   │   │   ├── StatsCards.tsx
│   │   │   ├── EventTrendChart.tsx
│   │   │   ├── SeverityDonut.tsx
│   │   │   ├── TopActorsTable.tsx
│   │   │   ├── ActivityHeatmap.tsx    # User activity calendar heatmap
│   │   │   └── QuotaUsageBar.tsx
│   │   ├── reports/
│   │   │   ├── ReportList.tsx
│   │   │   ├── GenerateReportModal.tsx
│   │   │   ├── ReportFilterBuilder.tsx
│   │   │   └── ReportStatusBadge.tsx
│   │   └── layout/
│   │       ├── AppShell.tsx            # Sidebar + top bar
│   │       ├── Sidebar.tsx
│   │       ├── TopBar.tsx
│   │       └── OrgSwitcher.tsx
│   ├── pages/
│   │   ├── auth/
│   │   │   ├── LoginPage.tsx
│   │   │   └── SamlCallbackPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── EventsPage.tsx              # Main search & list view
│   │   ├── EventDetailPage.tsx
│   │   ├── EntityHistoryPage.tsx
│   │   ├── UserActivityPage.tsx
│   │   ├── ReportsPage.tsx
│   │   ├── AlertsPage.tsx
│   │   ├── settings/
│   │   │   ├── OrganizationSettings.tsx
│   │   │   ├── ApplicationsSettings.tsx
│   │   │   ├── ApiKeysSettings.tsx
│   │   │   ├── TeamSettings.tsx        # Users & roles
│   │   │   ├── BillingSettings.tsx
│   │   │   └── RetentionSettings.tsx
│   │   └── onboarding/
│   │       ├── SignupPage.tsx
│   │       └── OnboardingWizard.tsx    # 4-step setup
│   ├── hooks/
│   │   ├── useAuditEvents.ts           # React Query + pagination
│   │   ├── useDashboardStats.ts
│   │   ├── useAuth.ts
│   │   ├── useTenant.ts
│   │   └── useInfiniteEvents.ts        # Infinite scroll for event list
│   ├── store/
│   │   ├── auth.store.ts               # Zustand
│   │   └── tenant.store.ts
│   ├── types/
│   │   ├── audit.types.ts
│   │   ├── auth.types.ts
│   │   └── api.types.ts
│   ├── utils/
│   │   ├── date.utils.ts
│   │   ├── severity.utils.ts           # Color mapping for severity
│   │   └── formatting.utils.ts
│   └── App.tsx
├── package.json
└── tailwind.config.js
```

### Key Page Designs

#### Dashboard Page

```tsx
// src/pages/DashboardPage.tsx
import { useQuery } from '@tanstack/react-query';
import { getDashboardStats, getEventTrends } from '../api/events.api';
import { StatsCards } from '../components/dashboard/StatsCards';
import { EventTrendChart } from '../components/dashboard/EventTrendChart';
import { SeverityDonut } from '../components/dashboard/SeverityDonut';
import { TopActorsTable } from '../components/dashboard/TopActorsTable';
import { QuotaUsageBar } from '../components/dashboard/QuotaUsageBar';

export function DashboardPage() {
  const { applicationId } = useTenant();

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['dashboard-stats', applicationId],
    queryFn: () => getDashboardStats(applicationId),
    refetchInterval: 30_000,  // auto-refresh every 30s
  });

  const { data: trends } = useQuery({
    queryKey: ['event-trends', applicationId],
    queryFn: () => getEventTrends({
      applicationId,
      granularity: 'DAY',
      startTime: subDays(new Date(), 30).toISOString(),
      endTime: new Date().toISOString(),
    }),
    refetchInterval: 60_000,
  });

  if (statsLoading) return <DashboardSkeleton />;

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">
            Real-time audit trail overview
          </p>
        </div>
        <ApplicationSelector />
      </div>

      {/* Quota warning banner */}
      {stats?.quotaUsedPercent > 80 && (
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 flex items-center gap-3">
          <AlertTriangle className="text-amber-600 w-5 h-5 flex-shrink-0" />
          <span className="text-sm text-amber-800">
            You've used {stats.quotaUsedPercent.toFixed(0)}% of your monthly event quota.{' '}
            <a href="/settings/billing" className="font-medium underline">Upgrade your plan</a>
          </span>
        </div>
      )}

      {/* Stat Cards Row */}
      <StatsCards stats={stats} />

      {/* Charts Row */}
      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-8">
          <EventTrendChart data={trends?.dataPoints ?? []} />
        </div>
        <div className="col-span-4">
          <SeverityDonut stats={stats} />
        </div>
      </div>

      {/* Bottom Row */}
      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-7">
          <TopActorsTable actors={stats?.topActors ?? []} />
        </div>
        <div className="col-span-5">
          <QuotaUsageBar
            used={stats?.totalEventsThisMonth ?? 0}
            limit={stats?.monthlyQuota ?? 0}
          />
        </div>
      </div>
    </div>
  );
}
```

#### Events Page (Main Search View)

```tsx
// src/pages/EventsPage.tsx
export function EventsPage() {
  const [filters, setFilters] = useState<EventFilters>({
    startTime: startOfDay(subDays(new Date(), 7)).toISOString(),
    endTime: endOfDay(new Date()).toISOString(),
  });
  const [selectedEvent, setSelectedEvent] = useState<AuditEvent | null>(null);
  const [pageToken, setPageToken] = useState<string | null>(null);

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['events', filters, pageToken],
    queryFn: () => searchEvents({ ...filters, pageToken, pageSize: 50 }),
    keepPreviousData: true,
    staleTime: 30_000,
  });

  return (
    <div className="flex h-[calc(100vh-64px)] overflow-hidden">
      {/* Main Panel */}
      <div className={cn("flex flex-col flex-1 overflow-hidden",
          selectedEvent ? "w-1/2" : "w-full")}>

        {/* Filter Bar */}
        <div className="p-4 border-b bg-white sticky top-0 z-10">
          <FilterBar
            filters={filters}
            onChange={(f) => { setFilters(f); setPageToken(null); }}
          />
        </div>

        {/* Results summary */}
        <div className="px-4 py-2 bg-gray-50 border-b flex items-center justify-between">
          <span className="text-sm text-gray-600">
            {isFetching ? 'Loading...' :
              `~${data?.totalEstimate?.toLocaleString('en-IN') ?? 0} events found`}
          </span>
          <button
            onClick={() => exportToCSV(filters)}
            className="text-sm text-blue-600 hover:text-blue-700 flex items-center gap-1"
          >
            <Download className="w-4 h-4" /> Export CSV
          </button>
        </div>

        {/* Event Table */}
        <div className="flex-1 overflow-auto">
          {isLoading ? (
            <EventTableSkeleton />
          ) : (
            <AuditEventTable
              events={data?.content ?? []}
              selectedEventId={selectedEvent?.eventId}
              onEventClick={setSelectedEvent}
            />
          )}
        </div>

        {/* Pagination */}
        <div className="p-4 border-t bg-white flex items-center justify-between">
          <span className="text-sm text-gray-500">
            Showing {data?.content.length ?? 0} events
          </span>
          <div className="flex gap-2">
            {pageToken && (
              <button onClick={() => setPageToken(null)}
                      className="btn-secondary text-sm">
                ← Back to Start
              </button>
            )}
            {data?.hasMore && (
              <button onClick={() => setPageToken(data.pageToken)}
                      className="btn-primary text-sm">
                Next Page →
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Detail Side Panel */}
      {selectedEvent && (
        <div className="w-1/2 border-l overflow-auto">
          <AuditEventDetail
            event={selectedEvent}
            onClose={() => setSelectedEvent(null)}
          />
        </div>
      )}
    </div>
  );
}
```

#### Audit Event Detail Component

```tsx
// src/components/audit/AuditEventDetail.tsx
export function AuditEventDetail({ event, onClose }: Props) {
  const severityConfig = getSeverityConfig(event.severity);

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b bg-white sticky top-0">
        <div className="flex items-center gap-3">
          <span className={cn("inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium",
              severityConfig.className)}>
            {event.severity}
          </span>
          <span className="text-sm font-medium text-gray-900">{event.action.name}</span>
        </div>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="flex-1 overflow-auto p-4 space-y-6">

        {/* Event ID + Time */}
        <div className="bg-gray-50 rounded-lg p-3 space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Event ID</span>
            <span className="font-mono text-gray-900 text-xs">{event.eventId}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Time</span>
            <span className="text-gray-900">
              {formatIST(event.eventTime)}
            </span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Outcome</span>
            <OutcomeBadge outcome={event.outcome} />
          </div>
          {event.correlationId && (
            <div className="flex justify-between text-sm">
              <span className="text-gray-500">Correlation ID</span>
              <span className="font-mono text-xs text-gray-700">{event.correlationId}</span>
            </div>
          )}
        </div>

        {/* Actor Section */}
        <div>
          <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">
            Actor
          </h3>
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
              <span className="text-blue-700 font-semibold text-sm">
                {event.actor.userName?.charAt(0) ?? '?'}
              </span>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-900">{event.actor.userName}</p>
              <p className="text-xs text-gray-500">{event.actor.userEmail}</p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div>
              <span className="text-gray-400">User ID</span>
              <p className="font-mono text-gray-700 mt-0.5">{event.actor.userId}</p>
            </div>
            <div>
              <span className="text-gray-400">IP Address</span>
              <p className="font-mono text-gray-700 mt-0.5">{event.actor.ipAddress ?? '—'}</p>
            </div>
          </div>
          {/* View all activity link */}
          <a href={`/users/${event.actor.userId}/activity`}
             className="mt-3 text-xs text-blue-600 hover:text-blue-700 flex items-center gap-1">
            View all activity by this user <ExternalLink className="w-3 h-3" />
          </a>
        </div>

        {/* Resource Section */}
        <div>
          <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">
            Resource
          </h3>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500">Type</span>
              <span className="font-medium text-gray-900">{event.resource.type}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">ID</span>
              <span className="font-mono text-xs text-gray-700">{event.resource.id}</span>
            </div>
            {event.resource.name && (
              <div className="flex justify-between">
                <span className="text-gray-500">Name</span>
                <span className="text-gray-900">{event.resource.name}</span>
              </div>
            )}
          </div>
          <a href={`/entities/${event.resource.type}/${event.resource.id}`}
             className="mt-3 text-xs text-blue-600 hover:text-blue-700 flex items-center gap-1">
            View entity history <ExternalLink className="w-3 h-3" />
          </a>
        </div>

        {/* Changes / Diff */}
        {event.changes && event.changes.length > 0 && (
          <div>
            <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">
              Changes ({event.changes.length})
            </h3>
            <DiffViewer changes={event.changes} />
          </div>
        )}

        {/* Metadata */}
        {event.metadata && Object.keys(event.metadata).length > 0 && (
          <div>
            <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">
              Metadata
            </h3>
            <div className="bg-gray-50 rounded-lg p-3 space-y-1">
              {Object.entries(event.metadata).map(([k, v]) => (
                <div key={k} className="flex justify-between text-xs">
                  <span className="text-gray-500 font-mono">{k}</span>
                  <span className="text-gray-700">{v}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tags */}
        {event.tags && event.tags.length > 0 && (
          <div>
            <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
              Tags
            </h3>
            <div className="flex flex-wrap gap-1.5">
              {event.tags.map(tag => (
                <span key={tag}
                      className="px-2 py-0.5 bg-blue-50 text-blue-700 rounded text-xs font-medium">
                  #{tag}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Footer Actions */}
      <div className="p-4 border-t bg-white">
        <div className="flex gap-2">
          <button onClick={() => copyEventJson(event)}
                  className="flex-1 btn-secondary text-sm flex items-center justify-center gap-1">
            <Copy className="w-4 h-4" /> Copy JSON
          </button>
          <button onClick={() => openRawPayload(event)}
                  className="flex-1 btn-secondary text-sm flex items-center justify-center gap-1">
            <Code className="w-4 h-4" /> Raw Payload
          </button>
        </div>
      </div>
    </div>
  );
}
```

#### Diff Viewer Component

```tsx
// src/components/audit/DiffViewer.tsx
export function DiffViewer({ changes }: { changes: FieldChange[] }) {
  return (
    <div className="rounded-lg border overflow-hidden divide-y">
      {changes.map((change, i) => (
        <div key={i} className="text-xs">
          <div className="bg-gray-50 px-3 py-1.5 font-mono font-medium text-gray-700">
            {change.fieldName}
          </div>
          <div className="grid grid-cols-2 divide-x">
            <div className="bg-red-50 px-3 py-2">
              <div className="text-red-400 text-xs mb-1 font-semibold">BEFORE</div>
              <code className="text-red-700 break-all">
                {change.oldValue != null
                  ? JSON.stringify(change.oldValue)
                  : <span className="text-gray-400 italic">null</span>}
              </code>
            </div>
            <div className="bg-green-50 px-3 py-2">
              <div className="text-green-400 text-xs mb-1 font-semibold">AFTER</div>
              <code className="text-green-700 break-all">
                {change.newValue != null
                  ? JSON.stringify(change.newValue)
                  : <span className="text-gray-400 italic">null</span>}
              </code>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
```

#### Filter Bar Component

```tsx
// src/components/audit/FilterBar.tsx
export function FilterBar({ filters, onChange }: Props) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="space-y-3">
      {/* Primary row */}
      <div className="flex items-center gap-3">
        <div className="flex-1">
          <Input
            placeholder="Search by user, resource, action..."
            value={filters.query ?? ''}
            onChange={(e) => onChange({ ...filters, query: e.target.value })}
            icon={<Search className="w-4 h-4 text-gray-400" />}
          />
        </div>
        <DateRangePicker
          start={filters.startTime}
          end={filters.endTime}
          presets={[
            { label: 'Last 1 hour', value: 'P1H' },
            { label: 'Last 24 hours', value: 'P1D' },
            { label: 'Last 7 days', value: 'P7D' },
            { label: 'Last 30 days', value: 'P30D' },
            { label: 'This month', value: 'CM' },
            { label: 'Custom', value: 'custom' },
          ]}
          onChange={(start, end) => onChange({ ...filters, startTime: start, endTime: end })}
        />
        <button
          onClick={() => setExpanded(!expanded)}
          className="flex items-center gap-1 text-sm text-gray-600 hover:text-gray-900"
        >
          <SlidersHorizontal className="w-4 h-4" />
          Filters
          {activeFilterCount(filters) > 0 && (
            <span className="ml-1 bg-blue-600 text-white text-xs rounded-full w-4 h-4
                           flex items-center justify-center">
              {activeFilterCount(filters)}
            </span>
          )}
        </button>
      </div>

      {/* Expanded filter row */}
      {expanded && (
        <div className="flex items-center gap-3 pt-2 border-t border-gray-100">
          <MultiSelect
            label="Action Type"
            options={ACTION_TYPES}
            value={filters.actionTypes ?? []}
            onChange={(v) => onChange({ ...filters, actionTypes: v })}
          />
          <MultiSelect
            label="Severity"
            options={SEVERITY_OPTIONS}
            value={filters.severities ?? []}
            onChange={(v) => onChange({ ...filters, severities: v })}
          />
          <MultiSelect
            label="Outcome"
            options={OUTCOME_OPTIONS}
            value={filters.outcomes ?? []}
            onChange={(v) => onChange({ ...filters, outcomes: v })}
          />
          <Input
            placeholder="Resource type..."
            value={filters.resourceType ?? ''}
            onChange={(e) => onChange({ ...filters, resourceType: e.target.value })}
            className="w-36"
          />
          <button
            onClick={() => onChange({ startTime: filters.startTime, endTime: filters.endTime })}
            className="text-sm text-gray-500 hover:text-gray-700"
          >
            Clear all
          </button>
        </div>
      )}
    </div>
  );
}
```

### React Query Configuration

```tsx
// src/api/client.ts
import axios from 'axios';
import { useAuthStore } from '../store/auth.store';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30_000,
});

// Attach JWT on every request
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const orgId = useAuthStore.getState().organizationId;
  if (orgId) {
    config.headers['X-Organization-Id'] = orgId;
  }
  return config;
});

// Token refresh on 401
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      try {
        await useAuthStore.getState().refreshToken();
        return apiClient(original);
      } catch {
        useAuthStore.getState().logout();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// src/main.tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      retry: 2,
      refetchOnWindowFocus: false,
    },
  },
});
```

### Sidebar Navigation Structure

```tsx
const NAV_ITEMS = [
  {
    label: 'Dashboard',
    icon: LayoutDashboard,
    href: '/dashboard',
    roles: ['ADMIN', 'AUDITOR', 'VIEWER'],
  },
  {
    label: 'Audit Events',
    icon: ClipboardList,
    href: '/events',
    roles: ['ADMIN', 'AUDITOR', 'VIEWER'],
  },
  {
    label: 'Reports',
    icon: FileText,
    href: '/reports',
    roles: ['ADMIN', 'AUDITOR'],
  },
  {
    label: 'Alerts',
    icon: Bell,
    href: '/alerts',
    roles: ['ADMIN', 'AUDITOR'],
  },
  {
    label: 'Replay',
    icon: RotateCcw,
    href: '/replay',
    roles: ['ADMIN'],
  },
  {
    divider: true,
    label: 'Settings',
  },
  {
    label: 'Organization',
    icon: Building,
    href: '/settings/organization',
    roles: ['ADMIN'],
  },
  {
    label: 'Applications',
    icon: AppWindow,
    href: '/settings/applications',
    roles: ['ADMIN'],
  },
  {
    label: 'API Keys',
    icon: Key,
    href: '/settings/api-keys',
    roles: ['ADMIN', 'DEVELOPER'],
  },
  {
    label: 'Team',
    icon: Users,
    href: '/settings/team',
    roles: ['ADMIN'],
  },
  {
    label: 'Billing',
    icon: CreditCard,
    href: '/settings/billing',
    roles: ['ADMIN'],
  },
];
```

---

## 12. Security Architecture

### RBAC Permission Matrix

| Permission | OWNER | ADMIN | AUDITOR | VIEWER | DEVELOPER |
|---|:---:|:---:|:---:|:---:|:---:|
| Ingest events | ✓ | ✓ | — | — | ✓ |
| View events | ✓ | ✓ | ✓ | ✓ | — |
| Export/Reports | ✓ | ✓ | ✓ | — | — |
| Replay events | ✓ | ✓ | — | — | — |
| Manage API Keys | ✓ | ✓ | — | — | ✓ |
| Manage Team | ✓ | ✓ | — | — | — |
| Manage Applications | ✓ | ✓ | — | — | — |
| Billing | ✓ | — | — | — | — |
| Delete Organization | ✓ | — | — | — | — |

### Data Isolation

- Every Cassandra query includes `organization_id` as partition key — no cross-tenant data access possible by design
- API Key is always validated against organization before query execution
- Row-level security enforced at service layer, not DB layer (Cassandra doesn't support RLS)
- All PII fields (userEmail, ipAddress, userName) can be masked for VIEWER role

### Threat Model Controls

```
Threat: Tenant data leakage
Control: organization_id in every Cassandra partition key
         + API Key → Org binding enforced in filter chain

Threat: Quota abuse / DDoS
Control: Redis rate limiter (sliding window)
         + API Gateway rate limiting (Kong)
         + Monthly quota enforced before Kafka publish

Threat: Event tampering
Control: Immutable Cassandra writes (no UPDATE/DELETE)
         + SHA-256 hash of raw_payload stored with event
         + S3 versioning for reports

Threat: Credential theft
Control: JWT short expiry (15 min) + refresh rotation
         + API keys bcrypt-hashed in DB
         + MFA support (TOTP)

Threat: Replay attacks
Control: Idempotency key (24h Redis TTL)
         + JWT jti blacklist on logout

Threat: Audit log tampering by admin
Control: Admin actions are themselves audited
         + Immutable Cassandra + S3 WORM (Object Lock)
         + Separate audit of admin actions to S3 CloudTrail
```

---

## 13. Deployment Architecture (AWS)

```
Region: ap-south-1 (Mumbai)

VPC
├── Public Subnets (3 AZs)
│   ├── Application Load Balancer (HTTPS/443)
│   └── NAT Gateway
├── Private Subnets — Application Tier (3 AZs)
│   ├── EKS Node Group
│   │   ├── audithub-ingestion-service    (3 pods, HPA: 3-20)
│   │   ├── audithub-query-service        (3 pods, HPA: 3-15)
│   │   ├── audithub-storage-service      (3 pods, HPA: 3-10)
│   │   ├── audithub-report-service       (2 pods, fixed)
│   │   ├── audithub-auth-service         (2 pods, HPA: 2-8)
│   │   ├── audithub-tenant-service       (2 pods)
│   │   └── audithub-frontend-service     (2 pods + CloudFront CDN)
│   └── Amazon MSK (Managed Kafka, 3 brokers, m5.large)
├── Private Subnets — Data Tier (3 AZs)
│   ├── Amazon Keyspaces (Managed Cassandra, serverless)
│   │   OR self-managed Cassandra on EC2 (r6g.2xlarge × 3)
│   ├── Amazon RDS PostgreSQL (db.r6g.large, Multi-AZ)
│   └── Amazon ElastiCache Redis (cache.r6g.large, cluster mode, 3 shards)
└── S3
    ├── audithub-reports-prod (reports + WORM Object Lock for compliance)
    └── audithub-static (frontend assets)

CloudFront CDN → S3 (frontend)
Route 53 → ALB (api.audithub.in)
ACM → TLS certificates
Secrets Manager → DB passwords, JWT secrets, API keys
CloudWatch → Logs, metrics, alarms
X-Ray → Distributed tracing
```

### Kubernetes Deployment Manifest (Ingestion Service)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ingestion-service
  namespace: audithub
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ingestion-service
  template:
    metadata:
      labels:
        app: ingestion-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
        - name: ingestion-service
          image: audithub/ingestion-service:latest
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: POSTGRES_HOST
              valueFrom:
                secretKeyRef:
                  name: db-secrets
                  key: host
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secrets
                  key: password
            - name: KAFKA_BROKERS
              value: "b-1.msk.ap-south-1.amazonaws.com:9092,b-2.msk.ap-south-1.amazonaws.com:9092"
            - name: CASSANDRA_HOSTS
              valueFrom:
                configMapKeyRef:
                  name: cassandra-config
                  key: hosts
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            initialDelaySeconds: 15
            periodSeconds: 5
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ingestion-service-hpa
  namespace: audithub
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ingestion-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

---

## 14. Compliance & Data Retention

### DPDP Act 2023 Compliance

```
Data Minimization:
- Audit events capture only what's needed for trail
- PII fields (email, name) can be pseudonymized at rest
- No financial account numbers stored in changes by default

Consent & Purpose:
- Tenants explicitly configure what events are captured
- Purpose documented in application settings

Retention:
- Default 90 days for STARTER
- Configurable per org (up to 10 years for Enterprise)
- Cassandra TTL auto-expires data — no manual purge jobs needed
- Right to Erasure: purge by actor_user_id (Cassandra delete by partition key)

Data Residency:
- All data stays in ap-south-1 (Mumbai) by default
- No cross-region replication unless explicitly opted in
```

### RBI IT Framework Compliance

```
Audit Trail Requirement (RBI Circular 2021):
- User access to sensitive systems must be logged
- Log WHO / WHAT / WHEN / FROM WHERE
- Tamper-evident storage
- Minimum 5-year retention for banking transactions

AuditHub Mapping:
- actor.userId / actor.userEmail → WHO
- action.name + resource.type/id → WHAT
- eventTime → WHEN
- actor.ipAddress + actor.userAgent → FROM WHERE
- Cassandra immutability + S3 WORM → Tamper-evident
- Enterprise plan supports 7-year retention

SEBI Audit Trail Circular (June 2022):
- BSE/NSE member software must maintain audit trails
- Every change to order management must be logged
- Supports SEBI report template out of the box
```

### Data Retention Implementation

```java
@Service
@RequiredArgsConstructor
public class TenantRetentionService {

    private final OrganizationCacheService orgCache;

    private static final Map<String, Integer> PLAN_RETENTION_DAYS = Map.of(
            "FREE",          7,
            "STARTER",       90,
            "PROFESSIONAL",  365,
            "ENTERPRISE",    2555  // 7 years
    );

    /**
     * Returns TTL in seconds for Cassandra writes.
     * Enterprise orgs can override via custom retention setting.
     */
    public int getRetentionTtlSeconds(UUID orgId) {
        Organization org = orgCache.get(orgId);
        int days = org.getRetentionDays() > 0
                ? org.getRetentionDays()
                : PLAN_RETENTION_DAYS.getOrDefault(org.getPlan(), 90);
        return (int) Duration.ofDays(days).toSeconds();
    }
}
```

---

## 15. SDK Design (Java Client)

### Maven Dependency

```xml
<dependency>
    <groupId>in.audithub</groupId>
    <artifactId>audithub-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Auto-Configuration

```yaml
# application.yml — client app
audithub:
  enabled: true
  api-key: ah_live_xK9mP3qR7nL2vB8jT5wY1uA4sE6hG0fD
  application-id: app-uuid-here
  base-url: https://api.audithub.in
  async: true             # fire-and-forget (recommended for production)
  batch-size: 100         # batch up to 100 events before flushing
  flush-interval-ms: 500  # flush every 500ms even if batch not full
  timeout-seconds: 5
  retry:
    max-attempts: 3
    backoff-multiplier: 2.0
```

### Annotation-Driven Audit

```java
// ─────────────────────────────────────────────────────────────────
// Audit Annotation
// ─────────────────────────────────────────────────────────────────

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    String action();                      // e.g. "loan.approved"
    String actionType() default "UPDATE"; // CREATE/UPDATE/DELETE/READ
    String resourceType();                // e.g. "LoanApplication"
    String severity() default "LOW";
    String description() default "";
    boolean captureArgs() default false;  // capture method args as metadata
    boolean captureResult() default false;
}

// ─────────────────────────────────────────────────────────────────
// AOP Aspect
// ─────────────────────────────────────────────────────────────────

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditHubClient auditHubClient;
    private final AuditContextProvider contextProvider;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Instant start = Instant.now();
        Object result = null;
        Throwable error = null;

        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            try {
                AuditContext ctx = contextProvider.current();
                AuditEventRequest event = buildEvent(pjp, audited, ctx, result, error);
                auditHubClient.send(event);
            } catch (Exception e) {
                // Never let audit failure break business logic
                log.error("AuditHub SDK error: {}", e.getMessage());
            }
        }
    }

    private AuditEventRequest buildEvent(ProceedingJoinPoint pjp, Audited audited,
                                          AuditContext ctx, Object result, Throwable error) {
        // Extract resource ID from method signature using convention:
        // First parameter annotated with @AuditResourceId
        String resourceId = extractResourceId(pjp);

        Map<String, String> metadata = new HashMap<>();
        if (audited.captureArgs()) {
            addArgsToMetadata(pjp, metadata);
        }

        return AuditEventRequest.builder()
                .actor(Actor.builder()
                        .userId(ctx.getUserId())
                        .userEmail(ctx.getUserEmail())
                        .userName(ctx.getUserName())
                        .ipAddress(ctx.getIpAddress())
                        .sessionId(ctx.getSessionId())
                        .build())
                .action(Action.builder()
                        .type(ActionType.valueOf(audited.actionType()))
                        .name(audited.action())
                        .description(audited.description())
                        .build())
                .resource(Resource.builder()
                        .type(audited.resourceType())
                        .id(resourceId)
                        .build())
                .outcome(error == null ? Outcome.SUCCESS : Outcome.FAILURE)
                .severity(Severity.valueOf(audited.severity()))
                .correlationId(ctx.getCorrelationId())
                .metadata(metadata)
                .build();
    }
}

// ─────────────────────────────────────────────────────────────────
// Usage in Client Application (e.g. HDFC NetBanking)
// ─────────────────────────────────────────────────────────────────

@Service
public class LoanService {

    @Audited(
        action = "loan.status.approved",
        actionType = "UPDATE",
        resourceType = "LoanApplication",
        severity = "HIGH",
        description = "Loan application approved"
    )
    public LoanApplication approveLoan(@AuditResourceId String loanId,
                                        BigDecimal amount) {
        // Business logic — audit is automatic
        return loanRepository.approve(loanId, amount);
    }

    @Audited(
        action = "loan.application.deleted",
        actionType = "DELETE",
        resourceType = "LoanApplication",
        severity = "CRITICAL"
    )
    public void deleteLoan(@AuditResourceId String loanId, String reason) {
        loanRepository.delete(loanId);
    }
}

// ─────────────────────────────────────────────────────────────────
// Programmatic Usage (for complex scenarios with field-level diff)
// ─────────────────────────────────────────────────────────────────

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AuditHubClient auditHub;

    public Account updateCreditLimit(String accountId, BigDecimal oldLimit,
                                      BigDecimal newLimit) {
        Account updated = accountRepository.updateCreditLimit(accountId, newLimit);

        auditHub.send(AuditEventRequest.builder()
                .actor(AuditContextHolder.getActor())
                .action(Action.of(ActionType.UPDATE, "account.credit_limit.changed"))
                .resource(Resource.of("Account", accountId, "Account #" + accountId))
                .changes(List.of(
                    FieldChange.of("creditLimit", oldLimit, newLimit)
                ))
                .outcome(Outcome.SUCCESS)
                .severity(Severity.HIGH)
                .metadata(Map.of("currency", "INR"))
                .tags(List.of("account", "credit", "branch-operation"))
                .build());

        return updated;
    }
}
```

### Async Batch Client

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditHubClient implements Closeable {

    private final AuditHubProperties props;
    private final RestClient restClient;

    // Bounded queue — drop on overflow rather than block business logic
    private final BlockingQueue<AuditEventRequest> queue =
            new ArrayBlockingQueue<>(10_000);
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    @PostConstruct
    public void start() {
        // Flush every N ms
        scheduler.scheduleAtFixedRate(
                this::flush,
                props.getFlushIntervalMs(),
                props.getFlushIntervalMs(),
                TimeUnit.MILLISECONDS);
    }

    public void send(AuditEventRequest event) {
        if (props.isAsync()) {
            boolean offered = queue.offer(event);
            if (!offered) {
                log.warn("AuditHub queue full — dropping event for {}",
                        event.getResource().getId());
            }
        } else {
            sendImmediate(event);
        }
    }

    private void flush() {
        List<AuditEventRequest> batch = new ArrayList<>(props.getBatchSize());
        queue.drainTo(batch, props.getBatchSize());

        if (batch.isEmpty()) return;

        try {
            if (batch.size() == 1) {
                sendImmediate(batch.get(0));
            } else {
                sendBatch(batch);
            }
        } catch (Exception e) {
            log.error("AuditHub flush failed for {} events: {}",
                    batch.size(), e.getMessage());
            // Events are lost — acceptable for audit SDK (business logic must not break)
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
        flush(); // Drain remaining events on shutdown
    }
}
```

---

## Appendix: SLAs & Capacity Planning

| Metric | Starter | Professional | Enterprise |
|---|---|---|---|
| Ingestion latency (p99) | < 200ms | < 100ms | < 50ms |
| Query latency (p99) | < 2s | < 500ms | < 200ms |
| Report generation | < 5 min | < 2 min | < 30s |
| Availability | 99.9% | 99.95% | 99.99% |
| RPO | 1 hour | 15 min | 5 min |
| RTO | 4 hours | 1 hour | 15 min |

### Capacity Estimates

```
100 Enterprise tenants × 10M events/month = 1B events/month
= ~385 events/second sustained
= ~4,000 events/second peak (10× headroom)

Storage per event: ~2KB (raw) + 500B (index tables) ≈ 2.5KB
1B events × 2.5KB = 2.5TB/month

Cassandra nodes needed:
  - 2.5TB/month × 12 months × replication factor 3 = 90TB/year
  - 6 × i3en.2xlarge (7.5TB NVMe each) = 45TB usable per DC
  - 2 DCs = 90TB — sufficient for 1 year

Kafka throughput:
  - 4,000 events/sec × 2KB = 8MB/s ingestion
  - 3 × kafka.m5.large handles 50MB/s — comfortable headroom
```

---

*Document version 1.0 — AuditHub System Design — June 2025*
*© AuditHub. Confidential and proprietary.*
