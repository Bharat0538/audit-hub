import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Zap, Send, CheckCircle2, AlertCircle, RefreshCw, Beaker, ChevronDown, ChevronUp } from 'lucide-react';
import { applicationsApi, apiKeysApi } from '../api/organizations.api';
import toast from 'react-hot-toast';
import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace('/v1', '') || 'http://localhost:8080';

// ─── Preset event templates ─────────────────────────────────────────────────
const PRESETS = [
  {
    label: 'User Login',
    color: 'blue',
    payload: {
      actor: { userId: 'USR-001', userEmail: 'alice@example.com', userName: 'Alice Johnson', ipAddress: '192.168.1.10' },
      action: { type: 'READ', name: 'user.login', description: 'User logged in successfully' },
      resource: { type: 'Session', id: 'SESS-001', name: 'Web Session' },
      outcome: 'SUCCESS', severity: 'LOW',
    },
  },
  {
    label: 'Data Export',
    color: 'amber',
    payload: {
      actor: { userId: 'USR-002', userEmail: 'bob@example.com', userName: 'Bob Smith', ipAddress: '10.0.0.5' },
      action: { type: 'READ', name: 'report.exported', description: 'User exported customer data as CSV' },
      resource: { type: 'Report', id: 'RPT-2024-06', name: 'Customer Data Export' },
      outcome: 'SUCCESS', severity: 'MEDIUM',
    },
  },
  {
    label: 'Account Created',
    color: 'green',
    payload: {
      actor: { userId: 'ADMIN-001', userEmail: 'admin@example.com', userName: 'System Admin', ipAddress: '127.0.0.1' },
      action: { type: 'CREATE', name: 'account.created', description: 'New savings account opened' },
      resource: { type: 'BankAccount', id: `ACC-${Date.now()}`, name: 'Savings Account' },
      changes: [{ fieldName: 'balance', oldValue: null, newValue: '10000' }, { fieldName: 'status', oldValue: null, newValue: 'ACTIVE' }],
      outcome: 'SUCCESS', severity: 'MEDIUM',
    },
  },
  {
    label: 'Failed Payment',
    color: 'red',
    payload: {
      actor: { userId: 'USR-099', userEmail: 'user@example.com', userName: 'John Doe', ipAddress: '203.0.113.42' },
      action: { type: 'CREATE', name: 'payment.failed', description: 'Payment declined due to insufficient funds' },
      resource: { type: 'Payment', id: `PAY-${Date.now()}`, name: 'Wire Transfer' },
      outcome: 'FAILURE', severity: 'HIGH',
    },
  },
  {
    label: 'Record Deleted',
    color: 'red',
    payload: {
      actor: { userId: 'ADMIN-002', userEmail: 'ops@example.com', userName: 'Ops Team', ipAddress: '10.1.1.100' },
      action: { type: 'DELETE', name: 'record.deleted', description: 'Portfolio record permanently deleted' },
      resource: { type: 'Portfolio', id: `PORT-${Date.now()}`, name: 'Investment Portfolio' },
      outcome: 'SUCCESS', severity: 'CRITICAL',
    },
  },
  {
    label: 'Config Change',
    color: 'purple',
    payload: {
      actor: { userId: 'SYSADM-001', userEmail: 'sysadmin@example.com', userName: 'Sys Admin', ipAddress: '192.168.0.1' },
      action: { type: 'UPDATE', name: 'config.changed', description: 'System configuration updated' },
      resource: { type: 'SystemConfig', id: 'CONFIG-MAIN', name: 'Main Config' },
      changes: [{ fieldName: 'maxRetries', oldValue: '3', newValue: '5' }],
      outcome: 'SUCCESS', severity: 'HIGH',
    },
  },
];

const SEVERITY_COLORS: Record<string, string> = {
  LOW: 'bg-green-100 text-green-700',
  MEDIUM: 'bg-amber-100 text-amber-700',
  HIGH: 'bg-orange-100 text-orange-700',
  CRITICAL: 'bg-red-100 text-red-700',
};

const PRESET_COLORS: Record<string, string> = {
  blue: 'border-blue-200 bg-blue-50 hover:bg-blue-100',
  amber: 'border-amber-200 bg-amber-50 hover:bg-amber-100',
  green: 'border-green-200 bg-green-50 hover:bg-green-100',
  red: 'border-red-200 bg-red-50 hover:bg-red-100',
  purple: 'border-purple-200 bg-purple-50 hover:bg-purple-100',
};

interface IngestResult {
  preset: string;
  status: 'success' | 'error';
  message: string;
  timestamp: string;
}

export function TestEventsPage() {
  const qc = useQueryClient();
  const [selectedApp, setSelectedApp] = useState<string>('');
  const [selectedKey, setSelectedKey] = useState<string>('');
  const [results, setResults] = useState<IngestResult[]>([]);
  const [batchCount, setBatchCount] = useState(3);
  const [showJson, setShowJson] = useState(false);
  const [customJson, setCustomJson] = useState('');
  const [sending, setSending] = useState<string | null>(null);

  const { data: apps = [] } = useQuery({ queryKey: ['applications'], queryFn: applicationsApi.list });
  const { data: keys = [] } = useQuery({ queryKey: ['api-keys'], queryFn: apiKeysApi.list });

  // Only WRITE / ADMIN keys for reference display
  const writeKeys = keys.filter((k: any) => k.isActive && (k.keyType === 'WRITE' || k.keyType === 'ADMIN'));

  // Auto-select first app
  const effectiveApp = selectedApp || (apps[0]?.id ?? '');

  const ingestEvent = async (presetLabel: string, payload: object) => {
    if (!effectiveApp) { toast.error('Please select an application first'); return; }
    if (!selectedKey) { toast.error('Please paste your API key first'); return; }
    if (!selectedKey.startsWith('ah_')) { toast.error('API key must start with "ah_"'); return; }

    setSending(presetLabel);
    try {
      const body = { applicationId: effectiveApp, ...payload };
      await axios.post(`${BASE_URL}/v1/ingest/events`, body, {
        headers: { 'X-API-Key': selectedKey, 'Content-Type': 'application/json' },
      });
      setResults(prev => [{
        preset: presetLabel,
        status: 'success' as const,
        message: 'Event ingested successfully',
        timestamp: new Date().toISOString(),
      }, ...prev].slice(0, 20));
      toast.success(`"${presetLabel}" event sent!`);
      // Invalidate dashboard after sending
      qc.invalidateQueries({ queryKey: ['dashboard-stats'] });
    } catch (e: any) {
      const msg = e.response?.data?.error?.message || e.message || 'Failed';
      setResults(prev => [{
        preset: presetLabel,
        status: 'error' as const,
        message: msg,
        timestamp: new Date().toISOString(),
      }, ...prev].slice(0, 20));
      toast.error(`Failed: ${msg}`);
    } finally {
      setSending(null);
    }
  };

  const ingestBatch = async () => {
    if (!effectiveApp) { toast.error('Please select an application first'); return; }
    if (!selectedKey) { toast.error('Please paste your API key first'); return; }
    if (!selectedKey.startsWith('ah_')) { toast.error('API key must start with "ah_"'); return; }

    setSending('batch');
    const picks = PRESETS.slice(0, batchCount);
    const events = picks.map(p => ({ applicationId: effectiveApp, ...p.payload }));
    try {
      await axios.post(`${BASE_URL}/v1/ingest/events/batch`, { events }, {
        headers: { 'X-API-Key': selectedKey, 'Content-Type': 'application/json' },
      });
      setResults(prev => [{
        preset: `Batch (${batchCount} events)`,
        status: 'success' as const,
        message: `${batchCount} events ingested via batch API`,
        timestamp: new Date().toISOString(),
      }, ...prev].slice(0, 20));
      toast.success(`Batch of ${batchCount} events sent!`);
      qc.invalidateQueries({ queryKey: ['dashboard-stats'] });
    } catch (e: any) {
      const msg = e.response?.data?.error?.message || e.message;
      toast.error(`Batch failed: ${msg}`);
    } finally {
      setSending(null);
    }
  };

  const ingestCustom = async () => {
    try {
      const parsed = JSON.parse(customJson);
      await ingestEvent('Custom Event', parsed);
    } catch {
      toast.error('Invalid JSON');
    }
  };

  return (
    <div className="p-6 max-w-5xl space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center shadow-lg">
          <Beaker className="h-5 w-5 text-white" />
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Test Event Generator</h1>
          <p className="text-sm text-gray-500">Send test audit events directly from the UI to see them reflected in your dashboard</p>
        </div>
      </div>

      {/* Config row */}
      <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm space-y-4">
        <h2 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
          <Zap className="h-4 w-4 text-amber-500" />
          Configuration
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* App selector */}
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1.5">Application</label>
            <select
              value={selectedApp}
              onChange={(e) => setSelectedApp(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none bg-white"
            >
              <option value="">— Select Application —</option>
              {apps.map((app: any) => (
                <option key={app.id} value={app.id}>{app.name} ({app.environment})</option>
              ))}
            </select>
            {apps.length === 0 && (
              <p className="text-xs text-amber-600 mt-1">No applications yet. Go to Settings → Applications to create one.</p>
            )}
          </div>

          {/* API Key paste input */}
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1.5">API Key (paste your key)</label>
            <input
              type="password"
              value={selectedKey}
              onChange={(e) => setSelectedKey(e.target.value)}
              placeholder="Paste your ah_live_... key here"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none font-mono"
            />
            {writeKeys.length > 0 && (
              <p className="text-xs text-gray-400 mt-1">
                Keys on account: {writeKeys.map((k: any) => <span key={k.id} className="font-mono bg-gray-100 px-1 rounded">{k.keyPrefix}••••</span>)}
                {' '}— copy the full key from Settings → API Keys (shown only at creation)
              </p>
            )}
            {writeKeys.length === 0 && (
              <p className="text-xs text-amber-600 mt-1">No active WRITE keys. Go to Settings → API Keys to create one.</p>
            )}
          </div>
        </div>

        {effectiveApp && selectedKey && selectedKey.startsWith('ah_') && (
          <div className="flex items-center gap-2 text-xs text-green-700 bg-green-50 border border-green-200 rounded-lg px-3 py-2">
            <CheckCircle2 className="h-3.5 w-3.5" />
            Ready to send events. Select a preset below or use the batch sender.
          </div>
        )}
      </div>

      {/* Preset events */}
      <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-gray-700 mb-4">Quick Presets — Click to Send</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {PRESETS.map((preset) => (
            <button
              key={preset.label}
              onClick={() => ingestEvent(preset.label, preset.payload)}
              disabled={!selectedApp || !selectedKey || sending === preset.label}
              className={`relative rounded-xl border p-4 text-left transition-all duration-150 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer ${PRESET_COLORS[preset.color]}`}
            >
              {sending === preset.label && (
                <div className="absolute inset-0 flex items-center justify-center rounded-xl bg-white/60">
                  <RefreshCw className="h-5 w-5 animate-spin text-gray-500" />
                </div>
              )}
              <div className="flex items-start justify-between gap-2">
                <p className="text-sm font-semibold text-gray-800">{preset.label}</p>
                <span className={`text-2xs font-bold px-1.5 py-0.5 rounded-full ${SEVERITY_COLORS[preset.payload.severity]}`}>
                  {preset.payload.severity}
                </span>
              </div>
              <p className="text-xs text-gray-500 mt-1">{preset.payload.action.type} · {preset.payload.resource.type}</p>
              <p className="text-2xs text-gray-400 mt-0.5 truncate">{preset.payload.action.description}</p>
              <div className="mt-2 flex items-center gap-1 text-xs font-medium text-gray-600">
                <Send className="h-3 w-3" /> Send Event
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Batch sender */}
      <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-gray-700 mb-3">Batch Event Sender</h2>
        <p className="text-xs text-gray-500 mb-4">Send multiple events at once to quickly populate your dashboard with data.</p>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <label className="text-sm text-gray-600">Events count:</label>
            <input
              type="range" min={1} max={6} value={batchCount}
              onChange={(e) => setBatchCount(Number(e.target.value))}
              className="w-28 accent-blue-600"
            />
            <span className="text-sm font-bold text-blue-600 w-4">{batchCount}</span>
          </div>
          <button
            onClick={ingestBatch}
            disabled={!selectedApp || !selectedKey || sending === 'batch'}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
          >
            {sending === 'batch' ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
            Send {batchCount} Events
          </button>
        </div>
      </div>

      {/* Custom JSON */}
      <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <button
          onClick={() => setShowJson(!showJson)}
          className="flex w-full items-center justify-between text-sm font-semibold text-gray-700 cursor-pointer"
        >
          Custom JSON Event
          {showJson ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </button>
        {showJson && (
          <div className="mt-4 space-y-3">
            <p className="text-xs text-gray-500">Paste a raw event payload (without <code>applicationId</code> — it will be added automatically).</p>
            <textarea
              value={customJson}
              onChange={(e) => setCustomJson(e.target.value)}
              rows={10}
              placeholder={JSON.stringify({
                actor: { userId: 'USR-001', userEmail: 'user@example.com', userName: 'Test User', ipAddress: '127.0.0.1' },
                action: { type: 'CREATE', name: 'custom.event', description: 'My custom test event' },
                resource: { type: 'TestResource', id: 'TEST-001', name: 'Test Resource' },
                outcome: 'SUCCESS', severity: 'LOW',
              }, null, 2)}
              className="w-full rounded-lg border border-gray-300 bg-gray-900 px-4 py-3 font-mono text-xs text-green-400 focus:outline-none focus:border-blue-500 resize-none"
            />
            <button
              onClick={ingestCustom}
              disabled={!selectedApp || !selectedKey || !customJson || !!sending}
              className="flex items-center gap-2 rounded-lg bg-violet-600 px-4 py-2 text-sm font-semibold text-white hover:bg-violet-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
            >
              <Send className="h-4 w-4" /> Send Custom Event
            </button>
          </div>
        )}
      </div>

      {/* Results log */}
      {results.length > 0 && (
        <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-gray-700">Event Log</h2>
            <button onClick={() => setResults([])} className="text-xs text-gray-400 hover:text-red-500 cursor-pointer">Clear</button>
          </div>
          <div className="space-y-2 max-h-64 overflow-y-auto">
            {results.map((r, i) => (
              <div key={i} className={`flex items-start gap-3 rounded-lg p-3 text-xs ${r.status === 'success' ? 'bg-green-50 border border-green-100' : 'bg-red-50 border border-red-100'}`}>
                {r.status === 'success'
                  ? <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 shrink-0" />
                  : <AlertCircle className="h-4 w-4 text-red-500 mt-0.5 shrink-0" />}
                <div className="flex-1">
                  <p className="font-semibold text-gray-800">{r.preset}</p>
                  <p className="text-gray-500">{r.message}</p>
                </div>
                <span className="text-gray-400 shrink-0">{new Date(r.timestamp).toLocaleTimeString()}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tip box */}
      <div className="rounded-xl border border-blue-100 bg-blue-50 p-4">
        <p className="text-xs font-semibold text-blue-800 mb-1">💡 How to see events in the Dashboard</p>
        <ol className="text-xs text-blue-700 space-y-1 list-decimal list-inside">
          <li>Select your Application and a WRITE API Key above</li>
          <li>Click any preset or send a batch</li>
          <li>Navigate to <strong>Dashboard</strong> — stats update immediately</li>
          <li>Go to <strong>Audit Events</strong> to search and filter your test events</li>
        </ol>
      </div>
    </div>
  );
}
