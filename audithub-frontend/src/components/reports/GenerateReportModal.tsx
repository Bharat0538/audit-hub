import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Modal } from '../ui/Modal';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { reportsApi } from '../../api/reports.api';
import { subDays } from 'date-fns';
import toast from 'react-hot-toast';

const schema = z.object({
  name:      z.string().min(1, 'Name is required'),
  format:    z.enum(['PDF', 'CSV', 'XLSX']),
  startTime: z.string(),
  endTime:   z.string(),
});

type FormData = z.infer<typeof schema>;

interface Props { open: boolean; onClose: () => void; }

export function GenerateReportModal({ open, onClose }: Props) {
  const qc = useQueryClient();
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      format:    'PDF',
      startTime: subDays(new Date(), 30).toISOString().split('T')[0],
      endTime:   new Date().toISOString().split('T')[0],
    },
  });

  const mutation = useMutation({
    mutationFn: (data: FormData) => reportsApi.generate({
      name:    data.name,
      format:  data.format,
      filters: { startTime: data.startTime + 'T00:00:00Z', endTime: data.endTime + 'T23:59:59Z' },
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['reports'] });
      toast.success('Report generation started');
      onClose();
    },
    onError: () => toast.error('Failed to start report generation'),
  });

  return (
    <Modal
      open={open} onClose={onClose} title="Generate Report" size="md"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={mutation.isPending} onClick={handleSubmit((d) => mutation.mutate(d))}>
            Generate
          </Button>
        </>
      }
    >
      <form className="space-y-4">
        <Input label="Report Name" error={errors.name?.message} {...register('name')}
          placeholder="e.g. RBI Audit Trail - June 2025" />

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">Format</label>
          <div className="flex gap-2">
            {(['PDF','CSV','XLSX'] as const).map((f) => (
              <label key={f} className="flex-1 cursor-pointer">
                <input type="radio" value={f} {...register('format')} className="sr-only peer" />
                <div className="rounded-lg border border-gray-200 py-2 text-center text-sm text-gray-600 peer-checked:border-blue-500 peer-checked:bg-blue-50 peer-checked:text-blue-700 hover:bg-gray-50 transition-colors">
                  {f}
                </div>
              </label>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Input label="From Date" type="date" {...register('startTime')} />
          <Input label="To Date"   type="date" {...register('endTime')} />
        </div>
      </form>
    </Modal>
  );
}
