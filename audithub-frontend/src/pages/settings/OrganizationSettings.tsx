import { useForm } from 'react-hook-form';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { organizationApi } from '../../api/auth.api';
import { Skeleton } from '../../components/ui/Skeleton';
import toast from 'react-hot-toast';

export function OrganizationSettings() {
  const qc = useQueryClient();
  const { data: org, isLoading } = useQuery({ queryKey: ['org-me'], queryFn: organizationApi.getCurrent });

  const { register, handleSubmit } = useForm({ values: { displayName: org?.displayName ?? '', contactEmail: org?.contactEmail ?? '' } });

  const mutation = useMutation({
    mutationFn: organizationApi.update,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['org-me'] }); toast.success('Saved'); },
  });

  if (isLoading) return <div className="p-6 space-y-4">{Array.from({length:4}).map((_,i)=><Skeleton key={i} className="h-10 w-full" />)}</div>;

  return (
    <div className="p-6 max-w-2xl">
      <h1 className="text-xl font-bold text-gray-900 mb-6">Organization Settings</h1>

      <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-card">
        <div className="mb-4 pb-4 border-b border-gray-100">
          <p className="text-xs text-gray-500 mb-1">Plan</p>
          <span className="inline-flex items-center rounded-full bg-blue-50 px-3 py-1 text-sm font-semibold text-blue-700">
            {org?.plan}
          </span>
        </div>

        <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-4">
          <Input label="Display Name" {...register('displayName')} />
          <Input label="Contact Email" type="email" {...register('contactEmail')} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Organization Slug</label>
            <input disabled value={org?.slug} className="w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-500 font-mono" />
            <p className="text-xs text-gray-400 mt-1">Slug cannot be changed after creation</p>
          </div>
          <Button type="submit" loading={mutation.isPending}>Save Changes</Button>
        </form>
      </div>
    </div>
  );
}
