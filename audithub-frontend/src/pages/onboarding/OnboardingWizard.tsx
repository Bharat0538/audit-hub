import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, Copy, ArrowRight } from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { copyToClipboard } from '../../utils/formatting.utils';
import toast from 'react-hot-toast';
import { cn } from '../../utils/formatting.utils';

const STEPS = ['Name your app', 'Copy API key', 'Send test event', 'View in dashboard'];

export function OnboardingWizard() {
  const [step, setStep] = useState(0);
  const [apiKey] = useState('ah_live_xK9mP3qR7nL2vB8jT5wY1uA4sE6hG0fD'); // from signup
  const navigate = useNavigate();

  const copy = () => {
    copyToClipboard(apiKey);
    toast.success('API key copied');
  };

  const CURL_EXAMPLE = `curl -X POST https://api.audithub.in/v1/ingest/events \\
  -H "X-API-Key: ${apiKey}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "actor":    { "userId": "test-user", "userName": "Test User" },
    "action":   { "type": "CREATE", "name": "test.event" },
    "resource": { "type": "TestResource", "id": "test-001" }
  }'`;

  return (
    <div className="max-w-2xl mx-auto px-6 py-12">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Welcome to AuditHub 🎉</h1>
        <p className="text-gray-500 mt-1">Let's get your first audit event flowing in 4 steps.</p>
      </div>

      {/* Steps indicator */}
      <div className="flex items-center mb-8">
        {STEPS.map((s, i) => (
          <div key={s} className="flex items-center flex-1">
            <div className={cn(
              'flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold transition-colors',
              i < step  ? 'bg-green-500 text-white' :
              i === step? 'bg-blue-600 text-white' :
                          'bg-gray-100 text-gray-400'
            )}>
              {i < step ? <CheckCircle className="h-4 w-4" /> : i + 1}
            </div>
            <span className={cn('ml-2 text-xs font-medium hidden sm:block',
              i === step ? 'text-blue-600' : i < step ? 'text-green-600' : 'text-gray-400')}>
              {s}
            </span>
            {i < STEPS.length - 1 && <div className={cn('flex-1 h-px mx-3', i < step ? 'bg-green-300' : 'bg-gray-200')} />}
          </div>
        ))}
      </div>

      {/* Step content */}
      <div className="rounded-2xl border border-gray-200 bg-white p-8 shadow-card">
        {step === 0 && (
          <div>
            <h2 className="text-lg font-semibold text-gray-900 mb-1">Your first application is ready</h2>
            <p className="text-sm text-gray-500 mb-6">We created "My First App" for you. You can rename or add more apps later in Settings.</p>
            <div className="rounded-lg bg-blue-50 border border-blue-100 p-4">
              <p className="text-sm font-medium text-blue-900">Default Application</p>
              <p className="text-xs text-blue-600 mt-0.5">My First App · PRODUCTION environment</p>
            </div>
            <Button className="mt-6" iconRight={<ArrowRight className="h-4 w-4" />}
              onClick={() => setStep(1)}>Next</Button>
          </div>
        )}
        {step === 1 && (
          <div>
            <h2 className="text-lg font-semibold text-gray-900 mb-1">Your API key</h2>
            <p className="text-sm text-gray-500 mb-6">This is shown <strong>only once</strong>. Copy and store it securely in your secrets manager.</p>
            <div className="flex items-center gap-2">
              <code className="flex-1 rounded-lg bg-gray-900 px-4 py-3 text-sm font-mono text-green-400 break-all">{apiKey}</code>
              <Button variant="secondary" icon={<Copy className="h-4 w-4" />} onClick={copy}>Copy</Button>
            </div>
            <Button className="mt-6" iconRight={<ArrowRight className="h-4 w-4" />}
              onClick={() => setStep(2)}>I've saved it</Button>
          </div>
        )}
        {step === 2 && (
          <div>
            <h2 className="text-lg font-semibold text-gray-900 mb-1">Send your first event</h2>
            <p className="text-sm text-gray-500 mb-4">Run this command in your terminal to send a test audit event:</p>
            <div className="relative">
              <pre className="rounded-lg bg-gray-900 p-4 text-xs font-mono text-gray-300 overflow-x-auto">{CURL_EXAMPLE}</pre>
              <button onClick={() => { copyToClipboard(CURL_EXAMPLE); toast.success('Copied'); }}
                className="absolute top-2 right-2 rounded p-1.5 text-gray-500 hover:bg-gray-700 hover:text-gray-300 cursor-pointer">
                <Copy className="h-3.5 w-3.5" />
              </button>
            </div>
            <Button className="mt-6" iconRight={<ArrowRight className="h-4 w-4" />}
              onClick={() => setStep(3)}>I ran it</Button>
          </div>
        )}
        {step === 3 && (
          <div className="text-center">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
              <CheckCircle className="h-8 w-8 text-green-600" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900 mb-2">You're all set!</h2>
            <p className="text-sm text-gray-500 mb-6">Your audit trail is live. Head to the dashboard to see your events.</p>
            <Button iconRight={<ArrowRight className="h-4 w-4" />} onClick={() => navigate('/dashboard')}>
              Go to Dashboard
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
