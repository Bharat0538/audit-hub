import { useState } from 'react';
import { Plus, Eye, EyeOff, Copy, Trash2, Key } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { apiKeysApi } from '../../api/organizations.api';
import type { ApiKey, ApiKeyCreated } from '../../types';
import { useForm } from 'react-hook-form';
import { copyToClipboard } from '../../utils/formatting.utils';
import { formatRelative } from '../../utils/date.utils';
import toast from 'react-hot-toast';

export function ApiKeysSettings() {
  const qc = useQueryClient();
  const [showModal, setShowModal]         = useState(false);
  const [newKey, setNewKey]               = useState<ApiKeyCreated | null>(null);
  const [showNewKey, setShowNewKey]       = useState(false);
  const { data: keys = [] }               = useQuery({ queryKey: ['api-keys'], queryFn: apiKeysApi.list });
  const { register, handleSubmit, reset } = useForm<{ name: string; keyType: 'WRITE' | 'READ' | 'ADMIN' }>();

  const create = useMutation({
    mutationFn: apiKeysApi.create,
    onSuccess: (data: ApiKeyCreated) => {
      qc.invalidateQueries({ queryKey: ['api-keys'] });
      setNewKey(data);
      setShowModal(false);
      reset();
    },
  });

  const revoke = useMutation({
    mutationFn: apiKeysApi.revoke,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['api-keys'] }); toast.success('Key revoked'); },
  });

  return (
    <div className="p-6 max-w-3xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">API Keys</h1>
          <p className="text-sm text-gray-500 mt-0.5">Keys are shown only once. Store them in your secrets manager.</p>
        </div>
        <Button icon={<Plus className="h-4 w-4" />} onClick={() => setShowModal(true)}>New Key</Button>
      </div>

      {/* New key banner */}
      {newKey && (
        <div className="rounded-xl border border-green-200 bg-green-50 p-4 mb-6">
          <p className="text-sm font-semibold text-green-900 mb-2 flex items-center gap-2">
            <Key className="h-4 w-4" /> Your new API key — copy it now, it won't be shown again
          </p>
          <div className="flex items-center gap-2">
            <code className="flex-1 rounded-lg bg-gray-900 px-3 py-2 text-xs font-mono text-green-400 break-all">
              {showNewKey ? newKey.plainTextKey : '•'.repeat(newKey.plainTextKey.length)}
            </code>
            <button onClick={() => setShowNewKey(!showNewKey)} className="text-gray-400 hover:text-gray-600 cursor-pointer">
              {showNewKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
            <button onClick={() => { copyToClipboard(newKey.plainTextKey); toast.success('Copied'); }}
              className="text-gray-400 hover:text-gray-600 cursor-pointer">
              <Copy className="h-4 w-4" />
            </button>
          </div>
          <button onClick={() => setNewKey(null)} className="text-xs text-green-700 mt-2 hover:underline cursor-pointer">Dismiss</button>
        </div>
      )}

      {/* Key list */}
      <div className="space-y-3">
        {keys.map((key: ApiKey) => (
          <div key={key.id} className="rounded-xl border border-gray-100 bg-white p-4 shadow-card flex items-center gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <p className="text-sm font-semibold text-gray-900">{key.name}</p>
                <Badge variant={key.keyType === 'ADMIN' ? 'red' : key.keyType === 'WRITE' ? 'amber' : 'green'}>
                  {key.keyType}
                </Badge>
                {!key.isActive && <Badge variant="gray">Revoked</Badge>}
              </div>
              <p className="text-xs font-mono text-gray-400">{key.keyPrefix}••••••••••••••••••</p>
              <p className="text-xs text-gray-400 mt-1">
                {key.lastUsedAt ? `Last used ${formatRelative(key.lastUsedAt)}` : 'Never used'}
              </p>
            </div>
            {key.isActive && (
              <button onClick={() => revoke.mutate(key.id)} className="text-gray-300 hover:text-red-500 transition-colors cursor-pointer">
                <Trash2 className="h-4 w-4" />
              </button>
            )}
          </div>
        ))}
      </div>

      <Modal open={showModal} onClose={() => setShowModal(false)} title="Create API Key" size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
            <Button loading={create.isPending} onClick={handleSubmit((d) => create.mutate(d))}>Create</Button>
          </>
        }
      >
        <form className="space-y-4">
          <Input label="Key Name" placeholder="e.g. NetBanking Write Key" {...register('name')} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Key Type</label>
            <select {...register('keyType')} className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none">
              <option value="WRITE">WRITE — Ingest events only</option>
              <option value="READ">READ — Query events only</option>
              <option value="ADMIN">ADMIN — Full access</option>
            </select>
          </div>
        </form>
      </Modal>
    </div>
  );
}
