import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { useAuthStore } from '../store/auth.store';
import { authApi } from '../api/auth.api';
import type { LoginRequest } from '../types';

export function useAuth() {
  const { user, isAuthenticated, setTokens, setUser, logout } = useAuthStore();
  const navigate = useNavigate();

  const loginMutation = useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (res) => {
      setTokens(res.accessToken, res.refreshToken);
      setUser(res.user as Parameters<typeof setUser>[0]);
      toast.success(`Welcome back, ${res.user.name}!`);
      navigate('/dashboard');
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || 'Invalid credentials');
    },
  });

  const logoutFn = useCallback(() => {
    logout();
    navigate('/login');
    toast.success('Logged out successfully');
  }, [logout, navigate]);

  return {
    user,
    isAuthenticated,
    login: loginMutation.mutate,
    isLoggingIn: loginMutation.isPending,
    logout: logoutFn,
    hasRole: (...roles: string[]) => user ? roles.includes(user.role) : false,
  };
}
