import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { Role } from '../types';
import { authApi } from '../api/auth.api';

interface AuthUser {
  id:             string;
  name:           string;
  email:          string;
  role:           Role;
  organizationId: string;
}

interface AuthState {
  accessToken:     string | null;
  refreshToken:    string | null;
  user:            AuthUser | null;
  isAuthenticated: boolean;

  setTokens:   (access: string, refresh: string) => void;
  setUser:     (user: AuthUser) => void;
  logout:      () => void;
  refreshTokenFn: () => Promise<string>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken:     null,
      refreshToken:    null,
      user:            null,
      isAuthenticated: false,

      setTokens: (access, refresh) =>
        set({ accessToken: access, refreshToken: refresh, isAuthenticated: true }),

      setUser: (user) => set({ user }),

      logout: () => {
        authApi.logout().catch(() => {});
        set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false });
      },

      refreshTokenFn: async () => {
        const token = get().refreshToken;
        if (!token) throw new Error('No refresh token');
        const res = await authApi.refreshToken(token);
        set({ accessToken: res.accessToken });
        return res.accessToken;
      },
    }),
    {
      name: 'audithub-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (s) => ({
        accessToken:     s.accessToken,
        refreshToken:    s.refreshToken,
        user:            s.user,
        isAuthenticated: s.isAuthenticated,
      }),
    }
  )
);
