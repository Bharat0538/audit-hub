import { Plus, Bell, ToggleLeft, ToggleRight, Trash2 } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { alertsApi } from '../api/organizations.api';
import type { AlertRule } from '../types';
import { EmptyState } from '../components/ui/EmptyState';
import toast from 'react-hot-toast';

export function AlertsPage() {
  const qc = useQueryClient();
  const { data: rules = [], isLoading } = useQuery({ queryKey: ['alert-rules'], queryFn: alertsApi.listRules });

  const toggle = useMutation({
    mutationFn: (rule: AlertRule) => alertsApi.updateRule(rule.id, { isActive: !rule.isActive }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['alert-rules'] }); toast.success('Rule updated'); },
  });

  const remove = useMutation({
    mutationFn: (id: string) => alertsApi.deleteRule(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['alert-rules'] }); toast.success('Rule deleted'); },
  });

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Alert Rules</h1>
          <p className="text-sm text-gray-500 mt-0.5">Get notified when suspicious patterns occur</p>
        </div>
        <Button icon={<Plus className="h-4 w-4" />}>New Rule</Button>
      </div>

      {isLoading ? null : !rules.length ? (
        <EmptyState
          icon={<Bell className="h-8 w-8" />}
          title="No alert rules yet"
          description="Create a rule to get notified when unusual activity is detected."
          action={{ label: 'Create first rule', onClick: () => {} }}
        />
      ) : (
        <div className="space-y-3">
          {rules.map((rule: AlertRule) => (
            <div key={rule.id} className="rounded-xl border border-gray-100 bg-white p-4 shadow-card flex items-center gap-4">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <p className="text-sm font-semibold text-gray-900">{rule.name}</p>
                  <Badge variant={rule.isActive ? 'green' : 'gray'}>{rule.isActive ? 'Active' : 'Paused'}</Badge>
                  <Badge variant={
                    rule.severity === 'CRITICAL' ? 'purple' :
                    rule.severity === 'HIGH' ? 'red' :
                    rule.severity === 'MEDIUM' ? 'amber' : 'green'
                  }>{rule.severity}</Badge>
                </div>
                <p className="text-xs text-gray-500">
                  {rule.conditionConfig.threshold} {rule.conditionConfig.actionType ?? 'any'} events in {rule.conditionConfig.windowMinutes} min
                </p>
                {rule.notificationChannels && rule.notificationChannels.length > 0 && (
                  <div className="flex gap-1 mt-2">
                    {rule.notificationChannels.map((ch) => (
                      <Badge key={ch} variant="blue" size="sm">{ch}</Badge>
                    ))}
                  </div>
                )}
              </div>
              <div className="flex items-center gap-2 flex-shrink-0">
                <button onClick={() => toggle.mutate(rule)} className="text-gray-400 hover:text-blue-600 transition-colors cursor-pointer">
                  {rule.isActive ? <ToggleRight className="h-5 w-5 text-blue-600" /> : <ToggleLeft className="h-5 w-5" />}
                </button>
                <button onClick={() => remove.mutate(rule.id)} className="text-gray-400 hover:text-red-600 transition-colors cursor-pointer">
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
