import { useState } from 'react';
import { Plus, Trash2, UserPlus } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { teamApi } from '../../api/organizations.api';
import type { UserWithRole } from '../../types';
import { useForm } from 'react-hook-form';
import { useAuth } from '../../hooks/useAuth';
import { formatRelative } from '../../utils/date.utils';
import toast from 'react-hot-toast';

const ROLE_COLORS: Record<string, 'purple' | 'red' | 'blue' | 'green' | 'gray'> = {
  OWNER: 'purple', ADMIN: 'red', AUDITOR: 'blue', VIEWER: 'green', DEVELOPER: 'gray'
};

export function TeamSettings() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [showInvite, setShowInvite] = useState(false);
  const { data: users = [] } = useQuery({ queryKey: ['team-users'], queryFn: teamApi.listUsers });
  const { register, handleSubmit, reset } = useForm<{ email: string; role: string }>();

  const invite = useMutation({
    mutationFn: teamApi.invite,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['team-users'] }); toast.success('Invite sent'); setShowInvite(false); reset(); },
  });

  const remove = useMutation({
    mutationFn: teamApi.removeUser,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['team-users'] }); toast.success('User removed'); },
  });

  return (
    <div className="p-6 max-w-3xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Team</h1>
          <p className="text-sm text-gray-500 mt-0.5">{users.length} members</p>
        </div>
        <Button icon={<UserPlus className="h-4 w-4" />} onClick={() => setShowInvite(true)}>Invite Member</Button>
      </div>

      <div className="rounded-xl border border-gray-100 bg-white shadow-card divide-y divide-gray-50">
        {users.map((u: UserWithRole) => (
          <div key={u.id} className="flex items-center gap-4 p-4">
            <div className="h-9 w-9 flex-shrink-0 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center">
              <span className="text-sm font-bold text-white">{u.name ? u.name.charAt(0).toUpperCase() : u.email.charAt(0).toUpperCase()}</span>
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <p className="text-sm font-medium text-gray-900">{u.name || u.email}</p>
                {u.id === user?.id && <Badge variant="gray" size="sm">You</Badge>}
              </div>
              <p className="text-xs text-gray-500">{u.email}</p>
              {u.lastLoginAt && <p className="text-xs text-gray-400 mt-0.5">Last login {formatRelative(u.lastLoginAt)}</p>}
            </div>
            <Badge variant={ROLE_COLORS[u.role] ?? 'gray'}>{u.role}</Badge>
            {u.id !== user?.id && (
              <button onClick={() => remove.mutate(u.id)} className="text-gray-300 hover:text-red-500 transition-colors ml-2 cursor-pointer">
                <Trash2 className="h-4 w-4" />
              </button>
            )}
          </div>
        ))}
      </div>

      <Modal open={showInvite} onClose={() => setShowInvite(false)} title="Invite Team Member" size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowInvite(false)}>Cancel</Button>
            <Button loading={invite.isPending} onClick={handleSubmit((d) => invite.mutate(d))}>Send Invite</Button>
          </>
        }
      >
        <form className="space-y-4">
          <Input label="Email Address" type="email" placeholder="colleague@company.com" {...register('email')} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Role</label>
            <select {...register('role')} className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none">
              <option value="ADMIN">Admin — Full management access</option>
              <option value="AUDITOR">Auditor — Can view + export events</option>
              <option value="VIEWER">Viewer — Read-only access</option>
              <option value="DEVELOPER">Developer — API key management</option>
            </select>
          </div>
        </form>
      </Modal>
    </div>
  );
}
