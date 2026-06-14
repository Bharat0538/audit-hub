import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { Application } from '../types';

interface TenantState {
  selectedAppId: string | null;
  applications:  Application[];
  setSelectedApp: (appId: string | null) => void;
  setApplications: (apps: Application[]) => void;
}

export const useTenantStore = create<TenantState>()(
  persist(
    (set) => ({
      selectedAppId:   null,
      applications:    [],
      setSelectedApp:  (appId) => set({ selectedAppId: appId }),
      setApplications: (apps)  => set({ applications: apps }),
    }),
    {
      name: 'audithub-tenant',
      storage: createJSONStorage(() => localStorage),
    }
  )
);
