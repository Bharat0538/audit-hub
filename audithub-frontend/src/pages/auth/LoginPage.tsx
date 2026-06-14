import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { ShieldCheck, Eye, EyeOff } from 'lucide-react';
import { useState } from 'react';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../hooks/useAuth';

const schema = z.object({
  email:    z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
  mfaCode:  z.string().optional(),
});

type FormData = z.infer<typeof schema>;

export function LoginPage() {
  const { login, isLoggingIn } = useAuth();
  const [showPwd, setShowPwd]   = useState(false);
  const [mfaRequired, setMfaRequired] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = (data: FormData) => {
    login(data, {
      onError: (err: { response?: { status?: number } }) => {
        if (err?.response?.status === 202) setMfaRequired(true);
      },
    });
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Left panel — branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-sidebar-bg flex-col justify-between p-12">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600">
            <ShieldCheck className="h-5 w-5 text-white" />
          </div>
          <span className="text-lg font-bold text-white">AuditHub</span>
        </div>
        <div>
          <blockquote className="text-2xl font-light text-gray-300 leading-relaxed mb-6">
            "Complete audit trail with every change tracked, every actor identified, every compliance box ticked."
          </blockquote>
          <div className="grid grid-cols-3 gap-4">
            {[['10B+','Events stored'],['99.99%','Uptime SLA'],['< 100ms','Ingestion latency']].map(([n,l]) => (
              <div key={l}>
                <p className="text-2xl font-bold text-white">{n}</p>
                <p className="text-xs text-gray-400 mt-0.5">{l}</p>
              </div>
            ))}
          </div>
        </div>
        <p className="text-xs text-gray-600">
          Trusted by banks, insurance companies, and SaaS platforms across India.
        </p>
      </div>

      {/* Right panel — form */}
      <div className="flex flex-1 items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-gray-900">Sign in</h1>
            <p className="text-sm text-gray-500 mt-1">
              Don't have an account?{' '}
              <Link to="/signup" className="text-blue-600 hover:text-blue-700 font-medium">Create one free</Link>
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Work Email" type="email" autoComplete="email"
              placeholder="priya@hdfc.com"
              error={errors.email?.message}
              {...register('email')}
            />
            <Input
              label="Password" type={showPwd ? 'text' : 'password'} autoComplete="current-password"
              error={errors.password?.message}
              iconRight={
                <button type="button" onClick={() => setShowPwd(!showPwd)} className="text-gray-400 hover:text-gray-600 cursor-pointer">
                  {showPwd ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              }
              {...register('password')}
            />
            {mfaRequired && (
              <Input
                label="Authenticator Code" type="text" maxLength={6}
                placeholder="000000" autoComplete="one-time-code"
                hint="Enter the 6-digit code from your authenticator app"
                {...register('mfaCode')}
              />
            )}
            <div className="flex justify-end">
              <Link to="/forgot-password" className="text-xs text-blue-600 hover:text-blue-700">Forgot password?</Link>
            </div>
            <Button type="submit" className="w-full" loading={isLoggingIn}>Sign In</Button>
          </form>
        </div>
      </div>
    </div>
  );
}
