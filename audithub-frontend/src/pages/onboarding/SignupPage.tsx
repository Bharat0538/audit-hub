import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { authApi } from '../../api/auth.api';
import toast from 'react-hot-toast';

const schema = z.object({
  name:        z.string().min(2, 'Company name must be at least 2 characters'),
  contactEmail:z.string().email('Enter a valid email'),
  password:    z.string().min(8, 'Password must be at least 8 characters')
                .regex(/[A-Z]/, 'Must contain uppercase')
                .regex(/[0-9]/, 'Must contain a number'),
  gstNumber:   z.string().optional(),
});

type FormData = z.infer<typeof schema>;

export function SignupPage() {
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const mutation = useMutation({
    mutationFn: authApi.signup,
    onSuccess: () => {
      toast.success('Check your email to verify your account!');
      navigate('/login');
    },
    onError: () => toast.error('Email already in use or server error'),
  });

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        <div className="flex items-center gap-2.5 mb-8">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600">
            <ShieldCheck className="h-5 w-5 text-white" />
          </div>
          <span className="text-lg font-bold text-gray-900">AuditHub</span>
        </div>

        <div className="rounded-2xl border border-gray-200 bg-white p-8 shadow-card">
          <h1 className="text-xl font-bold text-gray-900 mb-1">Create your account</h1>
          <p className="text-sm text-gray-500 mb-6">
            Already have an account? <Link to="/login" className="text-blue-600 hover:text-blue-700 font-medium">Sign in</Link>
          </p>

          <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-4">
            <Input label="Company Name" placeholder="HDFC Bank"
              error={errors.name?.message} {...register('name')} />
            <Input label="Work Email" type="email" placeholder="admin@hdfc.com"
              error={errors.contactEmail?.message} {...register('contactEmail')} />
            <Input label="Password" type="password"
              hint="Min 8 chars · 1 uppercase · 1 number"
              error={errors.password?.message} {...register('password')} />
            <Input label="GST Number (optional)" placeholder="27AABCH1234J1ZD"
              error={errors.gstNumber?.message} {...register('gstNumber')} />

            <Button type="submit" className="w-full mt-2" loading={mutation.isPending}>
              Create Free Account
            </Button>
          </form>

          <p className="text-xs text-gray-400 text-center mt-4">
            Free plan includes 10,000 events/month. No credit card required.
          </p>
        </div>
      </div>
    </div>
  );
}
