import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../../store/auth.store';
import { Loader } from 'lucide-react';

export function SamlCallbackPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { setTokens, setUser } = useAuthStore();

  useEffect(() => {
    const token   = params.get('access_token');
    const refresh = params.get('refresh_token');
    const userB64 = params.get('user');

    if (token && refresh && userB64) {
      setTokens(token, refresh);
      setUser(JSON.parse(atob(userB64)));
      navigate('/dashboard', { replace: true });
    } else {
      navigate('/login?error=sso_failed', { replace: true });
    }
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <Loader className="h-8 w-8 animate-spin text-blue-600 mx-auto mb-3" />
        <p className="text-sm text-gray-600">Completing sign-in…</p>
      </div>
    </div>
  );
}
