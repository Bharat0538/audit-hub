import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { subDays } from 'date-fns';
import { RotateCcw, AlertTriangle } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { replayApi } from '../api/organizations.api';
import toast from 'react-hot-toast';

export function ReplayPage() {
  const [submitted, setSubmitted] = useState(false);
  const { register, handleSubmit } = useForm({
    defaultValues: {
      startTime:   subDays(new Date(), 1).toISOString().split('T')[0],
      endTime:     new Date().toISOString().split('T')[0],
      targetTopic: '',
      reason:      '',
    },
  });

  const mutation = useMutation({
    mutationFn: replayApi.submit,
    onSuccess: () => { setSubmitted(true); toast.success('Replay job started'); },
    onError: () => toast.error('Failed to start replay'),
  });

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-900">Event Replay</h1>
        <p className="text-sm text-gray-500 mt-0.5">Re-publish historical audit events to a Kafka topic</p>
      </div>

      <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 flex gap-3 mb-6">
        <AlertTriangle className="h-4 w-4 text-amber-600 flex-shrink-0 mt-0.5" />
        <div className="text-sm text-amber-800">
          <p className="font-semibold mb-0.5">Admin only operation</p>
          <p className="text-xs">This action is audited. Replay events will be tagged with <code className="bg-amber-100 px-1 rounded">_replay:true</code>.</p>
        </div>
      </div>

      {submitted ? (
        <div className="rounded-xl border border-green-200 bg-green-50 p-8 text-center">
          <RotateCcw className="h-8 w-8 text-green-600 mx-auto mb-3 animate-spin" />
          <p className="text-sm font-semibold text-green-900">Replay job is running</p>
          <p className="text-xs text-green-700 mt-1">Events are being published to your Kafka topic. This may take a few minutes.</p>
          <Button variant="secondary" size="sm" className="mt-4" onClick={() => setSubmitted(false)}>New Replay</Button>
        </div>
      ) : (
        <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="rounded-xl border border-gray-100 bg-white p-6 shadow-card space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input label="From" type="date" {...register('startTime')} />
            <Input label="To"   type="date" {...register('endTime')} />
          </div>
          <Input label="Target Kafka Topic" placeholder="hdfc.fraud.audit.replay" {...register('targetTopic')} />
          <Input label="Reason (required for audit)" placeholder="Fraud detection system downtime recovery" {...register('reason')} />
          <Button type="submit" loading={mutation.isPending} icon={<RotateCcw className="h-4 w-4" />}>
            Start Replay
          </Button>
        </form>
      )}
    </div>
  );
}
