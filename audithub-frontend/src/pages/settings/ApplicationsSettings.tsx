import { useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { applicationsApi } from '../../api/organizations.api';
import type { Application } from '../../types';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';

export function ApplicationsSettings() {
  const qc = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const { data: apps = [] } = useQuery({ queryKey: ['applications'], queryFn: applicationsApi.list });
  const { register, handleSubmit, reset } = useForm<{ name: string; environment: string; description: string }>();

  const create = useMutation({
    mutationFn: applicationsApi.create,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['applications'] }); toast.success('Application created'); setShowModal(false); reset(); },
  });

  const remove = useMutation({
    mutationFn: applicationsApi.delete,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['applications'] }); toast.success('Application deleted'); },
  });

  const ENV_COLOR: Record<string, 'green' | 'amber' | 'blue'> = { PRODUCTION: 'green', STAGING: 'amber', DEVELOPMENT: 'blue' };

  return (
    <div className="p-6 max-w-3xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold text-gray-900">Applications</h1>
        <Button icon={<Plus className="h-4 w-4" />} onClick={() => setShowModal(true)}>New Application</Button>
      </div>

      <div className="space-y-3">
        {apps.map((app: Application) => (
          <div key={app.id} className="rounded-xl border border-gray-100 bg-white p-4 shadow-card flex items-center gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <p className="font-semibold text-gray-900 text-sm">{app.name}</p>
                <Badge variant={ENV_COLOR[app.environment] ?? 'gray'}>{app.environment}</Badge>
              </div>
              <p className="text-xs text-gray-400 font-mono">{app.id}</p>
              {app.description && <p className="text-xs text-gray-500 mt-1">{app.description}</p>}
            </div>
            <button onClick={() => remove.mutate(app.id)} className="text-gray-300 hover:text-red-500 transition-colors cursor-pointer">
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        ))}
      </div>

      <Modal open={showModal} onClose={() => setShowModal(false)} title="New Application" size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
            <Button loading={create.isPending} onClick={handleSubmit((d) => create.mutate(d))}>Create</Button>
          </>
        }
      >
        <form className="space-y-4">
          <Input label="Application Name" placeholder="NetBanking Portal" {...register('name')} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Environment</label>
            <select {...register('environment')} className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20">
              <option value="PRODUCTION">Production</option>
              <option value="STAGING">Staging</option>
              <option value="DEVELOPMENT">Development</option>
            </select>
          </div>
          <Input label="Description (optional)" placeholder="Customer-facing internet banking" {...register('description')} />
        </form>
      </Modal>
    </div>
  );
}
