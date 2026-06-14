# AuditHub — Complete Frontend Reference

> **Stack:** React 18 · TypeScript · Vite · Tailwind CSS · React Query · Zustand · Recharts · React Router v6  
> **Design:** Clean enterprise SaaS — slate sidebar, white content, blue-600 accent, red for critical severity  
> **API Base:** `https://api.audithub.in/v1` (monolith backend)

---

## Table of Contents

1. [Project Setup & Scripts](#1-project-setup--scripts)
2. [package.json](#2-packagejson)
3. [vite.config.ts](#3-viteconfigts)
4. [tailwind.config.js](#4-tailwindconfigjs)
5. [tsconfig.json](#5-tsconfigjson)
6. [public/index.html](#6-publicindexhtml)
7. [src/main.tsx](#7-srcmaintsx)
8. [src/App.tsx](#8-srcapptsx)
9. [Types](#9-types)
10. [API Layer](#10-api-layer)
11. [Store (Zustand)](#11-store-zustand)
12. [Hooks](#12-hooks)
13. [Utils](#13-utils)
14. [UI Components](#14-ui-components)
15. [Layout Components](#15-layout-components)
16. [Audit Components](#16-audit-components)
17. [Dashboard Components](#17-dashboard-components)
18. [Report Components](#18-report-components)
19. [Pages — Auth](#19-pages--auth)
20. [Pages — Onboarding](#20-pages--onboarding)
21. [Pages — Main App](#21-pages--main-app)
22. [Pages — Settings](#22-pages--settings)
23. [src/index.css](#23-srcindexcss)
24. [Environment Variables](#24-environment-variables)
25. [Additional Config Files](#25-additional-config-files)

---

## 1. Project Setup & Scripts

```bash
# Create project
npm create vite@latest audithub-frontend -- --template react-ts
cd audithub-frontend

# Install all dependencies
npm install \
  react-router-dom \
  @tanstack/react-query \
  @tanstack/react-query-devtools \
  axios \
  zustand \
  recharts \
  date-fns \
  react-hot-toast \
  lucide-react \
  clsx \
  tailwind-merge \
  @headlessui/react \
  react-hook-form \
  @hookform/resolvers \
  zod \
  react-day-picker \
  stomp-websocket-client \
  @stomp/stompjs

# Install dev dependencies  
npm install -D \
  tailwindcss \
  postcss \
  autoprefixer \
  @types/node \
  eslint \
  @typescript-eslint/parser \
  @typescript-eslint/eslint-plugin \
  eslint-plugin-react-hooks \
  prettier \
  @tailwindcss/forms

# Init Tailwind
npx tailwindcss init -p
```

---

## 2. package.json

```json
{
  "name": "audithub-frontend",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "format": "prettier --write src/**/*.{ts,tsx,css}",
    "type-check": "tsc --noEmit"
  },
  "dependencies": {
    "@headlessui/react": "^1.7.18",
    "@hookform/resolvers": "^3.3.4",
    "@stomp/stompjs": "^7.0.0",
    "@tanstack/react-query": "^5.18.1",
    "@tanstack/react-query-devtools": "^5.18.1",
    "axios": "^1.6.7",
    "clsx": "^2.1.0",
    "date-fns": "^3.3.1",
    "lucide-react": "^0.321.0",
    "react": "^18.2.0",
    "react-day-picker": "^8.10.0",
    "react-dom": "^18.2.0",
    "react-hook-form": "^7.50.1",
    "react-hot-toast": "^2.4.1",
    "react-router-dom": "^6.21.3",
    "recharts": "^2.10.3",
    "tailwind-merge": "^2.2.1",
    "zod": "^3.22.4",
    "zustand": "^4.5.0"
  },
  "devDependencies": {
    "@tailwindcss/forms": "^0.5.7",
    "@types/node": "^20.11.5",
    "@types/react": "^18.2.55",
    "@types/react-dom": "^18.2.19",
    "@typescript-eslint/eslint-plugin": "^6.21.0",
    "@typescript-eslint/parser": "^6.21.0",
    "@vitejs/plugin-react": "^4.2.1",
    "autoprefixer": "^10.4.17",
    "eslint": "^8.56.0",
    "eslint-plugin-react-hooks": "^4.6.0",
    "postcss": "^8.4.35",
    "prettier": "^3.2.4",
    "tailwindcss": "^3.4.1",
    "typescript": "^5.3.3",
    "vite": "^5.1.0"
  }
}
```

---

## 3. vite.config.ts

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          query: ['@tanstack/react-query'],
          charts: ['recharts'],
          ui: ['@headlessui/react', 'lucide-react'],
        },
      },
    },
  },
});
```

---

## 4. tailwind.config.js

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // AuditHub brand palette
        brand: {
          50:  '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
          950: '#172554',
        },
        // Sidebar dark slate
        sidebar: {
          bg:       '#0f172a',
          hover:    '#1e293b',
          active:   '#1e40af',
          border:   '#1e293b',
          text:     '#94a3b8',
          textHover:'#f1f5f9',
        },
        // Severity colors
        severity: {
          low:      '#22c55e',
          medium:   '#f59e0b',
          high:     '#ef4444',
          critical: '#7c3aed',
        },
        // Outcome colors
        outcome: {
          success: '#16a34a',
          failure: '#dc2626',
          partial: '#d97706',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'Consolas', 'monospace'],
      },
      fontSize: {
        '2xs': ['0.625rem', { lineHeight: '0.875rem' }],
      },
      boxShadow: {
        'card': '0 1px 3px 0 rgb(0 0 0 / 0.05), 0 1px 2px -1px rgb(0 0 0 / 0.05)',
        'card-hover': '0 4px 6px -1px rgb(0 0 0 / 0.07), 0 2px 4px -2px rgb(0 0 0 / 0.05)',
        'panel': '0 8px 25px -5px rgb(0 0 0 / 0.1)',
      },
      animation: {
        'fade-in': 'fadeIn 0.2s ease-out',
        'slide-in-right': 'slideInRight 0.25s ease-out',
        'slide-in-up': 'slideInUp 0.2s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideInRight: {
          '0%': { transform: 'translateX(20px)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' },
        },
        slideInUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
      },
    },
  },
  plugins: [require('@tailwindcss/forms')],
};
```

---

## 5. tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

---

## 6. public/index.html

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="description" content="AuditHub — Enterprise Audit Trail Platform for compliance and security" />
    <meta name="theme-color" content="#0f172a" />
    <!-- Inter font from Google -->
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link
      href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap"
      rel="stylesheet"
    />
    <title>AuditHub — Enterprise Audit Trail</title>
  </head>
  <body class="bg-gray-50 antialiased">
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

---

## 7. src/main.tsx

```typescript
import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { Toaster } from 'react-hot-toast';
import App from './App';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,          // 30s
      gcTime: 5 * 60_000,         // 5min
      retry: 2,
      refetchOnWindowFocus: false,
      refetchOnReconnect: true,
    },
    mutations: {
      retry: 0,
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 4000,
          style: {
            background: '#1e293b',
            color: '#f1f5f9',
            fontSize: '0.875rem',
            borderRadius: '8px',
            border: '1px solid #334155',
          },
          success: { iconTheme: { primary: '#22c55e', secondary: '#fff' } },
          error:   { iconTheme: { primary: '#ef4444', secondary: '#fff' } },
        }}
      />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </React.StrictMode>
);
```

---

## 8. src/App.tsx

```typescript
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/auth.store';
import { AppShell } from '@/components/layout/AppShell';

// Auth pages
import { LoginPage } from '@/pages/auth/LoginPage';
import { SamlCallbackPage } from '@/pages/auth/SamlCallbackPage';

// Onboarding
import { SignupPage } from '@/pages/onboarding/SignupPage';
import { OnboardingWizard } from '@/pages/onboarding/OnboardingWizard';

// App pages
import { DashboardPage } from '@/pages/DashboardPage';
import { EventsPage } from '@/pages/EventsPage';
import { EventDetailPage } from '@/pages/EventDetailPage';
import { EntityHistoryPage } from '@/pages/EntityHistoryPage';
import { UserActivityPage } from '@/pages/UserActivityPage';
import { ReportsPage } from '@/pages/ReportsPage';
import { AlertsPage } from '@/pages/AlertsPage';
import { ReplayPage } from '@/pages/ReplayPage';

// Settings pages
import { OrganizationSettings } from '@/pages/settings/OrganizationSettings';
import { ApplicationsSettings } from '@/pages/settings/ApplicationsSettings';
import { ApiKeysSettings } from '@/pages/settings/ApiKeysSettings';
import { TeamSettings } from '@/pages/settings/TeamSettings';
import { BillingSettings } from '@/pages/settings/BillingSettings';
import { RetentionSettings } from '@/pages/settings/RetentionSettings';

// Route guard
function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function RequireGuest({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  if (isAuthenticated) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<RequireGuest><LoginPage /></RequireGuest>} />
        <Route path="/signup" element={<RequireGuest><SignupPage /></RequireGuest>} />
        <Route path="/auth/saml/callback" element={<SamlCallbackPage />} />

        {/* Protected app routes */}
        <Route path="/" element={<RequireAuth><AppShell /></RequireAuth>}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="onboarding" element={<OnboardingWizard />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="events" element={<EventsPage />} />
          <Route path="events/:eventId" element={<EventDetailPage />} />
          <Route path="entities/:resourceType/:resourceId" element={<EntityHistoryPage />} />
          <Route path="users/:userId/activity" element={<UserActivityPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="alerts" element={<AlertsPage />} />
          <Route path="replay" element={<ReplayPage />} />
          {/* Settings */}
          <Route path="settings">
            <Route path="organization" element={<OrganizationSettings />} />
            <Route path="applications" element={<ApplicationsSettings />} />
            <Route path="api-keys" element={<ApiKeysSettings />} />
            <Route path="team" element={<TeamSettings />} />
            <Route path="billing" element={<BillingSettings />} />
            <Route path="retention" element={<RetentionSettings />} />
          </Route>
        </Route>

        {/* Catch-all */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
```

---

## 9. Types

### src/types/audit.types.ts

```typescript
export type ActionType =
  | 'CREATE' | 'UPDATE' | 'DELETE' | 'READ'
  | 'LOGIN' | 'LOGOUT' | 'EXPORT' | 'APPROVE'
  | 'REJECT' | 'TRANSFER' | 'CUSTOM';

export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type Outcome  = 'SUCCESS' | 'FAILURE' | 'PARTIAL';

export interface Actor {
  userId:      string;
  userEmail?:  string;
  userName?:   string;
  ipAddress?:  string;
  userAgent?:  string;
  sessionId?:  string;
}

export interface Action {
  type:         ActionType;
  name:         string;
  description?: string;
}

export interface Resource {
  type:  string;
  id:    string;
  name?: string;
  path?: string;
}

export interface FieldChange {
  fieldName: string;
  oldValue?: unknown;
  newValue?: unknown;
}

export interface AuditEvent {
  eventId:       string;
  organizationId:string;
  applicationId: string;
  eventTime:     string;   // ISO 8601
  actor:         Actor;
  action:        Action;
  resource:      Resource;
  changes:       FieldChange[];
  outcome:       Outcome;
  severity:      Severity;
  correlationId?:string;
  metadata?:     Record<string, string>;
  tags?:         string[];
  createdAt:     string;
}

export interface AuditEventPage {
  content:       AuditEvent[];
  pageToken?:    string | null;
  hasMore:       boolean;
  totalEstimate: number;
}

export interface EventFilters {
  applicationId?: string;
  startTime:      string;
  endTime:        string;
  actorUserId?:   string;
  actorUserEmail?:string;
  resourceType?:  string;
  resourceId?:    string;
  actionTypes?:   ActionType[];
  severities?:    Severity[];
  outcomes?:      Outcome[];
  tags?:          string[];
  query?:         string;
}

export interface DashboardStats {
  totalEventsToday:      number;
  totalEventsThisMonth:  number;
  quotaUsedPercent:      number;
  monthlyQuota:          number;
  criticalEventsToday:   number;
  failedEventsToday:     number;
  activeApplications:    number;
  topActors:             TopActor[];
  topResources:          TopResource[];
  eventsTrend:           TrendPoint[];
}

export interface TopActor {
  userId:     string;
  userName:   string;
  eventCount: number;
}

export interface TopResource {
  resourceType: string;
  eventCount:   number;
}

export interface TrendPoint {
  date:          string;   // "2025-06-13"
  count:         number;
  criticalCount: number;
  failureCount:  number;
}

export interface AlertRule {
  id:                   string;
  organizationId:       string;
  applicationId?:       string;
  name:                 string;
  description?:         string;
  conditionType:        'THRESHOLD' | 'PATTERN' | 'ANOMALY';
  conditionConfig:      AlertConditionConfig;
  severity:             Severity;
  notificationChannels: string[];
  isActive:             boolean;
  createdAt:            string;
}

export interface AlertConditionConfig {
  actionType?:    string;
  resourceType?:  string;
  actorUserId?:   string;
  threshold:      number;
  windowMinutes:  number;
  groupBy?:       string;
}

export interface ReplayRequest {
  filters: Partial<EventFilters>;
  targetTopic: string;
  reason:      string;
}
```

### src/types/auth.types.ts

```typescript
export type Role = 'OWNER' | 'ADMIN' | 'AUDITOR' | 'VIEWER' | 'DEVELOPER';

export interface User {
  id:            string;
  organizationId:string;
  email:         string;
  name:          string;
  avatarUrl?:    string;
  status:        'ACTIVE' | 'INVITED' | 'SUSPENDED';
  authProvider:  'LOCAL' | 'GOOGLE' | 'SAML';
  mfaEnabled:    boolean;
  lastLoginAt?:  string;
  createdAt:     string;
}

export interface UserWithRole extends User {
  role: Role;
}

export interface AuthTokens {
  accessToken:  string;
  refreshToken: string;
  expiresIn:    number;
  user: {
    id:    string;
    name:  string;
    email: string;
    role:  Role;
    organizationId: string;
  };
}

export interface LoginRequest {
  email:    string;
  password: string;
  mfaCode?: string;
}

export interface SignupRequest {
  name:          string;
  contactEmail:  string;
  password:      string;
  gstNumber?:    string;
}
```

### src/types/api.types.ts

```typescript
export interface ApiResponse<T> {
  success: boolean;
  data:    T;
  error?:  ApiError;
  traceId: string;
}

export interface ApiError {
  code:    string;
  message: string;
  details?: Record<string, string[]>;
}

export interface PageResponse<T> {
  content:        T[];
  pageToken?:     string | null;
  hasMore:        boolean;
  totalEstimate:  number;
}

export interface Organization {
  id:                  string;
  name:                string;
  slug:                string;
  displayName:         string;
  plan:                Plan;
  status:              string;
  contactEmail:        string;
  maxEventsPerMonth:   number;
  retentionDays:       number;
  createdAt:           string;
}

export type Plan = 'FREE' | 'STARTER' | 'PROFESSIONAL' | 'ENTERPRISE';

export interface Application {
  id:             string;
  organizationId: string;
  name:           string;
  slug:           string;
  description?:   string;
  environment:    'PRODUCTION' | 'STAGING' | 'DEVELOPMENT';
  status:         string;
  webhookUrl?:    string;
  createdAt:      string;
}

export interface ApiKey {
  id:          string;
  name:        string;
  keyPrefix:   string;
  keyType:     'WRITE' | 'READ' | 'ADMIN';
  isActive:    boolean;
  lastUsedAt?: string;
  expiresAt?:  string;
  createdAt:   string;
}

export interface ApiKeyCreated extends ApiKey {
  plainTextKey: string;
}

export interface GeneratedReport {
  id:            string;
  name:          string;
  status:        'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  format:        'PDF' | 'CSV' | 'XLSX';
  rowCount?:     number;
  fileSizeBytes?:number;
  downloadUrl?:  string;
  errorMessage?: string;
  requestedBy:   string;
  createdAt:     string;
  completedAt?:  string;
}

export interface UsageStats {
  plan:                string;
  currentPeriodStart:  string;
  currentPeriodEnd:    string;
  eventsUsed:          number;
  eventsLimit:         number;
  usagePercent:        number;
  storageUsedBytes:    number;
  retentionDays:       number;
}
```

---

## 10. API Layer

### src/api/client.ts

```typescript
import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/store/auth.store';
import toast from 'react-hot-toast';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/v1';

export const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor — attach token
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // Correlation ID for tracing
  config.headers['X-Correlation-ID'] = crypto.randomUUID();
  return config;
});

// Response interceptor — handle 401, errors
let isRefreshing = false;
let failedQueue: Array<{ resolve: (v: string) => void; reject: (e: unknown) => void }> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((p) => (error ? p.reject(error) : p.resolve(token!)));
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !original._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`;
          return apiClient(original);
        });
      }

      original._retry = true;
      isRefreshing = true;

      try {
        const newToken = await useAuthStore.getState().refreshToken();
        processQueue(null, newToken);
        original.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(original);
      } catch (refreshError) {
        processQueue(refreshError, null);
        useAuthStore.getState().logout();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // Show toast for non-auth errors
    if (error.response?.status !== 401) {
      const msg = (error.response?.data as { message?: string })?.message || 'Something went wrong';
      if (error.response?.status === 507) {
        toast.error('Monthly quota exceeded. Upgrade your plan.');
      } else if (error.response?.status === 429) {
        toast.error('Too many requests. Please slow down.');
      } else if (error.response?.status && error.response.status >= 500) {
        toast.error(`Server error: ${msg}`);
      }
    }

    return Promise.reject(error);
  }
);
```

### src/api/auth.api.ts

```typescript
import { apiClient } from './client';
import type { AuthTokens, LoginRequest, SignupRequest } from '@/types/auth.types';
import type { Organization } from '@/types/api.types';

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<AuthTokens>('/auth/login', data).then((r) => r.data),

  signup: (data: SignupRequest) =>
    apiClient.post<{ organizationId: string; userId: string; plainApiKey: string }>(
      '/organizations', data
    ).then((r) => r.data),

  logout: () => apiClient.post('/auth/logout'),

  refreshToken: (refreshToken: string) =>
    apiClient.post<{ accessToken: string; expiresIn: number }>(
      '/auth/refresh', { refreshToken }
    ).then((r) => r.data),

  forgotPassword: (email: string) =>
    apiClient.post('/auth/forgot-password', { email }),

  resetPassword: (token: string, newPassword: string) =>
    apiClient.post('/auth/reset-password', { token, newPassword }),

  verifyEmail: (token: string) =>
    apiClient.post<AuthTokens>('/auth/verify-email', { token }).then((r) => r.data),

  initiateSaml: (orgSlug: string) =>
    apiClient.get<{ redirectUrl: string }>(`/auth/saml/initiate?orgSlug=${orgSlug}`).then((r) => r.data),

  setupMfa: () =>
    apiClient.post<{ secret: string; qrCodeUrl: string; backupCodes: string[] }>(
      '/auth/mfa/setup'
    ).then((r) => r.data),

  confirmMfa: (code: string) => apiClient.post('/auth/mfa/confirm', { code }),
};

export const organizationApi = {
  getCurrent: () =>
    apiClient.get<Organization>('/organizations/me').then((r) => r.data),

  update: (data: Partial<Organization>) =>
    apiClient.patch<Organization>('/organizations/me', data).then((r) => r.data),

  getUsage: () =>
    apiClient.get('/organizations/me/usage').then((r) => r.data),
};
```

### src/api/events.api.ts

```typescript
import { apiClient } from './client';
import type { AuditEvent, AuditEventPage, EventFilters, DashboardStats, TrendPoint } from '@/types/audit.types';

export const eventsApi = {
  search: (filters: EventFilters & { pageToken?: string; pageSize?: number }) =>
    apiClient.get<AuditEventPage>('/events', { params: flattenFilters(filters) })
      .then((r) => r.data),

  getById: (eventId: string, applicationId: string, eventTime: string) =>
    apiClient.get<AuditEvent>(`/events/${eventId}`, {
      params: { applicationId, eventTime },
    }).then((r) => r.data),

  getEntityHistory: (
    resourceType: string,
    resourceId: string,
    params?: { startTime?: string; endTime?: string; pageToken?: string; pageSize?: number }
  ) =>
    apiClient.get<AuditEventPage>(`/events/entity/${resourceType}/${resourceId}`, { params })
      .then((r) => r.data),

  getUserActivity: (
    userId: string,
    params: { startTime: string; endTime: string; pageToken?: string; pageSize?: number }
  ) =>
    apiClient.get<AuditEventPage>(`/events/user/${userId}`, { params }).then((r) => r.data),

  getDashboardStats: (applicationId?: string) =>
    apiClient.get<DashboardStats>('/dashboard/stats', {
      params: applicationId ? { applicationId } : {},
    }).then((r) => r.data),

  getTrends: (params: {
    applicationId?: string;
    granularity?: 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';
    startTime: string;
    endTime: string;
  }) =>
    apiClient.get<{ dataPoints: TrendPoint[] }>('/dashboard/trends', { params })
      .then((r) => r.data),
};

function flattenFilters(filters: EventFilters & { pageToken?: string; pageSize?: number }) {
  const params: Record<string, unknown> = { ...filters };
  // Convert arrays to comma-separated for query params
  if (filters.actionTypes?.length) params.actionType = filters.actionTypes.join(',');
  if (filters.severities?.length)  params.severity   = filters.severities.join(',');
  if (filters.outcomes?.length)    params.outcome     = filters.outcomes.join(',');
  if (filters.tags?.length)        params.tags        = filters.tags.join(',');
  delete params.actionTypes;
  delete params.severities;
  delete params.outcomes;
  return params;
}
```

### src/api/reports.api.ts

```typescript
import { apiClient } from './client';
import type { GeneratedReport } from '@/types/api.types';
import type { EventFilters } from '@/types/audit.types';

export interface GenerateReportRequest {
  name:       string;
  templateId?: string;
  format:     'PDF' | 'CSV' | 'XLSX';
  filters:    Partial<EventFilters> & { startTime: string; endTime: string };
}

export const reportsApi = {
  generate: (data: GenerateReportRequest) =>
    apiClient.post<GeneratedReport>('/reports', data).then((r) => r.data),

  list: (params?: { page?: number; size?: number; status?: string }) =>
    apiClient.get<{ content: GeneratedReport[]; totalElements: number; totalPages: number }>(
      '/reports', { params }
    ).then((r) => r.data),

  getById: (reportId: string) =>
    apiClient.get<GeneratedReport>(`/reports/${reportId}`).then((r) => r.data),

  listTemplates: () =>
    apiClient.get<{ id: string; name: string; templateType: string; format: string }[]>(
      '/reports/templates'
    ).then((r) => r.data),
};
```

### src/api/organizations.api.ts

```typescript
import { apiClient } from './client';
import type { Application, ApiKey, ApiKeyCreated } from '@/types/api.types';
import type { UserWithRole } from '@/types/auth.types';

export const applicationsApi = {
  list: () =>
    apiClient.get<Application[]>('/applications').then((r) => r.data),

  create: (data: { name: string; environment: string; description?: string; webhookUrl?: string }) =>
    apiClient.post<Application>('/applications', data).then((r) => r.data),

  update: (id: string, data: Partial<Application>) =>
    apiClient.patch<Application>(`/applications/${id}`, data).then((r) => r.data),

  delete: (id: string) => apiClient.delete(`/applications/${id}`),
};

export const apiKeysApi = {
  list: () =>
    apiClient.get<ApiKey[]>('/api-keys').then((r) => r.data),

  create: (data: {
    name: string;
    applicationId?: string;
    keyType: 'WRITE' | 'READ' | 'ADMIN';
    scopes?: string[];
    expiresAt?: string;
  }) =>
    apiClient.post<ApiKeyCreated>('/api-keys', data).then((r) => r.data),

  revoke: (keyId: string) => apiClient.delete(`/api-keys/${keyId}`),
};

export const teamApi = {
  listUsers: () =>
    apiClient.get<UserWithRole[]>('/team/users').then((r) => r.data),

  invite: (data: { email: string; role: string; applicationId?: string }) =>
    apiClient.post('/team/invite', data),

  updateRole: (userId: string, role: string) =>
    apiClient.patch(`/team/users/${userId}/role`, { role }),

  removeUser: (userId: string) => apiClient.delete(`/team/users/${userId}`),
};

export const alertsApi = {
  listRules: () =>
    apiClient.get('/alerts/rules').then((r) => r.data),

  createRule: (data: unknown) =>
    apiClient.post('/alerts/rules', data).then((r) => r.data),

  updateRule: (id: string, data: unknown) =>
    apiClient.patch(`/alerts/rules/${id}`, data).then((r) => r.data),

  deleteRule: (id: string) => apiClient.delete(`/alerts/rules/${id}`),
};

export const replayApi = {
  submit: (data: unknown) =>
    apiClient.post('/replay', data).then((r) => r.data),

  listJobs: () =>
    apiClient.get('/replay').then((r) => r.data),
};
```

---

## 11. Store (Zustand)

### src/store/auth.store.ts

```typescript
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { Role } from '@/types/auth.types';
import { authApi } from '@/api/auth.api';

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
  refreshToken: () => Promise<string>;
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

      refreshToken: async () => {
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
```

### src/store/tenant.store.ts

```typescript
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { Application } from '@/types/api.types';

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
```

---

## 12. Hooks

### src/hooks/useAuth.ts

```typescript
import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { useAuthStore } from '@/store/auth.store';
import { authApi } from '@/api/auth.api';
import type { LoginRequest } from '@/types/auth.types';

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
    onError: (err: { response?: { data?: { message?: string } } }) => {
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
```

### src/hooks/useAuditEvents.ts

```typescript
import { useQuery } from '@tanstack/react-query';
import { eventsApi } from '@/api/events.api';
import type { EventFilters } from '@/types/audit.types';

export function useAuditEvents(
  filters: EventFilters,
  options?: { pageToken?: string; pageSize?: number; enabled?: boolean }
) {
  return useQuery({
    queryKey: ['events', filters, options?.pageToken, options?.pageSize],
    queryFn: () =>
      eventsApi.search({
        ...filters,
        pageToken: options?.pageToken,
        pageSize:  options?.pageSize ?? 50,
      }),
    enabled:        options?.enabled !== false,
    staleTime:      30_000,
    placeholderData: (prev) => prev,
  });
}

export function useAuditEvent(
  eventId: string,
  applicationId: string,
  eventTime: string
) {
  return useQuery({
    queryKey: ['event', eventId],
    queryFn: () => eventsApi.getById(eventId, applicationId, eventTime),
    staleTime: 5 * 60_000,
  });
}

export function useEntityHistory(
  resourceType: string,
  resourceId: string,
  options?: { startTime?: string; endTime?: string }
) {
  return useQuery({
    queryKey: ['entity-history', resourceType, resourceId, options],
    queryFn: () => eventsApi.getEntityHistory(resourceType, resourceId, options),
    staleTime: 60_000,
  });
}

export function useUserActivity(
  userId: string,
  params: { startTime: string; endTime: string }
) {
  return useQuery({
    queryKey: ['user-activity', userId, params],
    queryFn: () => eventsApi.getUserActivity(userId, params),
    staleTime: 30_000,
  });
}
```

### src/hooks/useDashboardStats.ts

```typescript
import { useQuery } from '@tanstack/react-query';
import { eventsApi } from '@/api/events.api';
import { useTenantStore } from '@/store/tenant.store';
import { subDays } from 'date-fns';

export function useDashboardStats() {
  const selectedAppId = useTenantStore((s) => s.selectedAppId);

  const stats = useQuery({
    queryKey: ['dashboard-stats', selectedAppId],
    queryFn: () => eventsApi.getDashboardStats(selectedAppId ?? undefined),
    refetchInterval: 30_000,
    staleTime: 20_000,
  });

  const trends = useQuery({
    queryKey: ['event-trends', selectedAppId],
    queryFn: () =>
      eventsApi.getTrends({
        applicationId: selectedAppId ?? undefined,
        granularity:   'DAY',
        startTime:     subDays(new Date(), 30).toISOString(),
        endTime:       new Date().toISOString(),
      }),
    refetchInterval: 60_000,
    staleTime: 55_000,
  });

  return { stats, trends };
}
```

### src/hooks/useTenant.ts

```typescript
import { useQuery } from '@tanstack/react-query';
import { applicationsApi } from '@/api/organizations.api';
import { useTenantStore } from '@/store/tenant.store';
import { useEffect } from 'react';

export function useTenant() {
  const { selectedAppId, applications, setSelectedApp, setApplications } = useTenantStore();

  const appsQuery = useQuery({
    queryKey: ['applications'],
    queryFn:  applicationsApi.list,
    staleTime: 5 * 60_000,
  });

  useEffect(() => {
    if (appsQuery.data) {
      setApplications(appsQuery.data);
      // Auto-select first app if none selected
      if (!selectedAppId && appsQuery.data.length > 0) {
        setSelectedApp(appsQuery.data[0].id);
      }
    }
  }, [appsQuery.data, selectedAppId, setApplications, setSelectedApp]);

  return {
    selectedAppId,
    selectedApp: applications.find((a) => a.id === selectedAppId) ?? null,
    applications,
    setSelectedApp,
    isLoading: appsQuery.isLoading,
  };
}
```

### src/hooks/useInfiniteEvents.ts

```typescript
import { useInfiniteQuery } from '@tanstack/react-query';
import { eventsApi } from '@/api/events.api';
import type { EventFilters } from '@/types/audit.types';

export function useInfiniteEvents(filters: EventFilters, pageSize = 50) {
  return useInfiniteQuery({
    queryKey: ['events-infinite', filters],
    queryFn: ({ pageParam }) =>
      eventsApi.search({ ...filters, pageToken: pageParam as string, pageSize }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.hasMore ? lastPage.pageToken ?? undefined : undefined,
    staleTime: 30_000,
  });
}
```

---

## 13. Utils

### src/utils/date.utils.ts

```typescript
import { format, formatDistanceToNow, parseISO, isValid } from 'date-fns';

export const IST_TIMEZONE = 'Asia/Kolkata';

export function formatIST(dateStr: string, fmt = 'dd MMM yyyy, HH:mm:ss'): string {
  try {
    const date = parseISO(dateStr);
    if (!isValid(date)) return dateStr;
    // Format in IST using Intl
    return new Intl.DateTimeFormat('en-IN', {
      timeZone: IST_TIMEZONE,
      day:    '2-digit',
      month:  'short',
      year:   'numeric',
      hour:   '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).format(date);
  } catch {
    return dateStr;
  }
}

export function formatRelative(dateStr: string): string {
  try {
    return formatDistanceToNow(parseISO(dateStr), { addSuffix: true });
  } catch {
    return dateStr;
  }
}

export function formatShort(dateStr: string): string {
  try {
    return format(parseISO(dateStr), 'dd MMM, HH:mm');
  } catch {
    return dateStr;
  }
}

export function toISOString(date: Date): string {
  return date.toISOString();
}

export function startOfDayISO(date: Date): string {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d.toISOString();
}

export function endOfDayISO(date: Date): string {
  const d = new Date(date);
  d.setHours(23, 59, 59, 999);
  return d.toISOString();
}
```

### src/utils/severity.utils.ts

```typescript
import type { Severity, Outcome } from '@/types/audit.types';

export interface SeverityConfig {
  label:   string;
  dotColor:string;
  badgeBg: string;
  badgeText:string;
  iconColor:string;
}

export const SEVERITY_CONFIG: Record<Severity, SeverityConfig> = {
  LOW: {
    label:     'Low',
    dotColor:  'bg-green-400',
    badgeBg:   'bg-green-50',
    badgeText: 'text-green-700',
    iconColor: 'text-green-500',
  },
  MEDIUM: {
    label:     'Medium',
    dotColor:  'bg-amber-400',
    badgeBg:   'bg-amber-50',
    badgeText: 'text-amber-700',
    iconColor: 'text-amber-500',
  },
  HIGH: {
    label:     'High',
    dotColor:  'bg-red-400',
    badgeBg:   'bg-red-50',
    badgeText: 'text-red-700',
    iconColor: 'text-red-500',
  },
  CRITICAL: {
    label:     'Critical',
    dotColor:  'bg-purple-500',
    badgeBg:   'bg-purple-50',
    badgeText: 'text-purple-700',
    iconColor: 'text-purple-600',
  },
};

export function getSeverityConfig(severity: Severity): SeverityConfig {
  return SEVERITY_CONFIG[severity] ?? SEVERITY_CONFIG.LOW;
}

export const OUTCOME_CONFIG: Record<Outcome, { label: string; badgeBg: string; badgeText: string }> = {
  SUCCESS: { label: 'Success', badgeBg: 'bg-green-50', badgeText: 'text-green-700' },
  FAILURE: { label: 'Failure', badgeBg: 'bg-red-50',   badgeText: 'text-red-700'   },
  PARTIAL: { label: 'Partial', badgeBg: 'bg-amber-50', badgeText: 'text-amber-700' },
};

export const ACTION_TYPE_LABELS: Record<string, string> = {
  CREATE:   'Create',
  UPDATE:   'Update',
  DELETE:   'Delete',
  READ:     'Read',
  LOGIN:    'Login',
  LOGOUT:   'Logout',
  EXPORT:   'Export',
  APPROVE:  'Approve',
  REJECT:   'Reject',
  TRANSFER: 'Transfer',
  CUSTOM:   'Custom',
};
```

### src/utils/formatting.utils.ts

```typescript
export function cn(...classes: (string | undefined | null | false)[]): string {
  return classes.filter(Boolean).join(' ');
}

export function formatNumber(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000)     return `${(n / 1_000).toFixed(1)}K`;
  return n.toLocaleString('en-IN');
}

export function formatBytes(bytes: number): string {
  if (bytes >= 1_073_741_824) return `${(bytes / 1_073_741_824).toFixed(1)} GB`;
  if (bytes >= 1_048_576)     return `${(bytes / 1_048_576).toFixed(1)} MB`;
  if (bytes >= 1_024)         return `${(bytes / 1_024).toFixed(1)} KB`;
  return `${bytes} B`;
}

export function maskEmail(email: string): string {
  const [local, domain] = email.split('@');
  return `${local[0]}***@${domain}`;
}

export function maskIp(ip: string): string {
  const parts = ip.split('.');
  if (parts.length === 4) return `${parts[0]}.${parts[1]}.*.*`;
  return ip;
}

export function truncate(str: string, maxLength = 50): string {
  if (str.length <= maxLength) return str;
  return `${str.slice(0, maxLength)}…`;
}

export function slugify(str: string): string {
  return str.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
}

export function copyToClipboard(text: string): Promise<void> {
  return navigator.clipboard.writeText(text);
}
```

---

## 14. UI Components

### src/components/ui/Button.tsx

```typescript
import { forwardRef } from 'react';
import { cn } from '@/utils/formatting.utils';

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost' | 'link';
type Size    = 'xs' | 'sm' | 'md' | 'lg';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?:    Size;
  loading?: boolean;
  icon?:    React.ReactNode;
  iconRight?:React.ReactNode;
}

const variantClasses: Record<Variant, string> = {
  primary:   'bg-blue-600 text-white hover:bg-blue-700 active:bg-blue-800 shadow-sm',
  secondary: 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50 shadow-sm',
  danger:    'bg-red-600 text-white hover:bg-red-700 active:bg-red-800 shadow-sm',
  ghost:     'text-gray-600 hover:bg-gray-100 hover:text-gray-900',
  link:      'text-blue-600 hover:text-blue-700 underline-offset-2 hover:underline p-0',
};

const sizeClasses: Record<Size, string> = {
  xs: 'px-2.5 py-1 text-xs rounded',
  sm: 'px-3 py-1.5 text-sm rounded-md',
  md: 'px-4 py-2 text-sm rounded-lg',
  lg: 'px-5 py-2.5 text-base rounded-lg',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size = 'md', loading, icon, iconRight, className, children, disabled, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center gap-2 font-medium transition-colors',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2',
        'disabled:pointer-events-none disabled:opacity-50',
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
      {...props}
    >
      {loading ? (
        <svg className="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      ) : icon}
      {children}
      {!loading && iconRight}
    </button>
  )
);
Button.displayName = 'Button';
```

### src/components/ui/Input.tsx

```typescript
import { forwardRef } from 'react';
import { cn } from '@/utils/formatting.utils';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?:    string;
  error?:    string;
  hint?:     string;
  icon?:     React.ReactNode;
  iconRight?:React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, icon, iconRight, className, id, ...props }, ref) => {
    const inputId = id || label?.toLowerCase().replace(/\s+/g, '-');
    return (
      <div className="w-full">
        {label && (
          <label htmlFor={inputId} className="block text-sm font-medium text-gray-700 mb-1.5">
            {label}
          </label>
        )}
        <div className="relative">
          {icon && (
            <div className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-gray-400">
              {icon}
            </div>
          )}
          <input
            ref={ref}
            id={inputId}
            className={cn(
              'block w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm',
              'placeholder:text-gray-400 text-gray-900',
              'focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20',
              'disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-500',
              error && 'border-red-400 focus:border-red-500 focus:ring-red-500/20',
              icon && 'pl-9',
              iconRight && 'pr-9',
              className
            )}
            {...props}
          />
          {iconRight && (
            <div className="absolute inset-y-0 right-3 flex items-center text-gray-400">
              {iconRight}
            </div>
          )}
        </div>
        {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
        {hint && !error && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
      </div>
    );
  }
);
Input.displayName = 'Input';
```

### src/components/ui/Badge.tsx

```typescript
import { cn } from '@/utils/formatting.utils';
import type { Severity, Outcome } from '@/types/audit.types';
import { getSeverityConfig, OUTCOME_CONFIG } from '@/utils/severity.utils';

interface BadgeProps {
  children: React.ReactNode;
  variant?: 'default' | 'blue' | 'green' | 'red' | 'amber' | 'purple' | 'gray';
  size?:    'sm' | 'md';
  dot?:     boolean;
  className?:string;
}

const variants = {
  default: 'bg-gray-100 text-gray-700',
  blue:    'bg-blue-50 text-blue-700',
  green:   'bg-green-50 text-green-700',
  red:     'bg-red-50 text-red-700',
  amber:   'bg-amber-50 text-amber-700',
  purple:  'bg-purple-50 text-purple-700',
  gray:    'bg-gray-100 text-gray-600',
};

export function Badge({ children, variant = 'default', size = 'sm', dot, className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full font-medium',
        size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-sm',
        variants[variant],
        className
      )}
    >
      {dot && <span className={cn('h-1.5 w-1.5 rounded-full', variants[variant].split(' ')[1].replace('text-', 'bg-'))} />}
      {children}
    </span>
  );
}

export function SeverityBadge({ severity }: { severity: Severity }) {
  const cfg = getSeverityConfig(severity);
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-semibold', cfg.badgeBg, cfg.badgeText)}>
      <span className={cn('h-1.5 w-1.5 rounded-full', cfg.dotColor)} />
      {cfg.label}
    </span>
  );
}

export function OutcomeBadge({ outcome }: { outcome: Outcome }) {
  const cfg = OUTCOME_CONFIG[outcome];
  return (
    <span className={cn('inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium', cfg.badgeBg, cfg.badgeText)}>
      {cfg.label}
    </span>
  );
}
```

### src/components/ui/Table.tsx

```typescript
import { cn } from '@/utils/formatting.utils';

interface TableProps {
  children:   React.ReactNode;
  className?: string;
}

export function Table({ children, className }: TableProps) {
  return (
    <div className={cn('overflow-x-auto', className)}>
      <table className="min-w-full divide-y divide-gray-200">{children}</table>
    </div>
  );
}

export function TableHeader({ children }: { children: React.ReactNode }) {
  return <thead className="bg-gray-50">{children}</thead>;
}

export function TableBody({ children }: { children: React.ReactNode }) {
  return <tbody className="divide-y divide-gray-100 bg-white">{children}</tbody>;
}

export function Th({ children, className }: { children?: React.ReactNode; className?: string }) {
  return (
    <th className={cn('px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider', className)}>
      {children}
    </th>
  );
}

export function Td({ children, className }: { children?: React.ReactNode; className?: string }) {
  return (
    <td className={cn('px-4 py-3 text-sm text-gray-900', className)}>{children}</td>
  );
}
```

### src/components/ui/Modal.tsx

```typescript
import { Fragment } from 'react';
import { Dialog, Transition } from '@headlessui/react';
import { X } from 'lucide-react';
import { cn } from '@/utils/formatting.utils';

interface ModalProps {
  open:      boolean;
  onClose:   () => void;
  title:     string;
  children:  React.ReactNode;
  size?:     'sm' | 'md' | 'lg' | 'xl';
  footer?:   React.ReactNode;
}

const sizeClasses = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
};

export function Modal({ open, onClose, title, children, size = 'md', footer }: ModalProps) {
  return (
    <Transition.Root show={open} as={Fragment}>
      <Dialog as="div" className="relative z-50" onClose={onClose}>
        <Transition.Child
          as={Fragment}
          enter="ease-out duration-200" enterFrom="opacity-0" enterTo="opacity-100"
          leave="ease-in duration-150" leaveFrom="opacity-100" leaveTo="opacity-0"
        >
          <div className="fixed inset-0 bg-gray-900/50 backdrop-blur-sm" />
        </Transition.Child>

        <div className="fixed inset-0 z-10 overflow-y-auto p-4 sm:p-8">
          <div className="flex min-h-full items-center justify-center">
            <Transition.Child
              as={Fragment}
              enter="ease-out duration-200" enterFrom="opacity-0 scale-95" enterTo="opacity-100 scale-100"
              leave="ease-in duration-150" leaveFrom="opacity-100 scale-100" leaveTo="opacity-0 scale-95"
            >
              <Dialog.Panel className={cn('w-full bg-white rounded-xl shadow-panel animate-fade-in', sizeClasses[size])}>
                <div className="flex items-center justify-between border-b border-gray-100 px-6 py-4">
                  <Dialog.Title className="text-base font-semibold text-gray-900">{title}</Dialog.Title>
                  <button onClick={onClose} className="text-gray-400 hover:text-gray-600 rounded-lg p-1 hover:bg-gray-100 transition-colors">
                    <X className="h-4 w-4" />
                  </button>
                </div>
                <div className="px-6 py-4">{children}</div>
                {footer && <div className="border-t border-gray-100 px-6 py-4 bg-gray-50 rounded-b-xl flex justify-end gap-2">{footer}</div>}
              </Dialog.Panel>
            </Transition.Child>
          </div>
        </div>
      </Dialog>
    </Transition.Root>
  );
}
```

### src/components/ui/Skeleton.tsx

```typescript
import { cn } from '@/utils/formatting.utils';

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse rounded bg-gray-200', className)} />;
}

export function SkeletonText({ lines = 3 }: { lines?: number }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton key={i} className={cn('h-4', i === lines - 1 ? 'w-3/4' : 'w-full')} />
      ))}
    </div>
  );
}

export function SkeletonCard() {
  return (
    <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-card">
      <Skeleton className="h-4 w-24 mb-4" />
      <Skeleton className="h-8 w-16 mb-2" />
      <Skeleton className="h-3 w-32" />
    </div>
  );
}

export function TableSkeleton({ rows = 10, cols = 6 }: { rows?: number; cols?: number }) {
  return (
    <div className="space-y-0">
      {Array.from({ length: rows }).map((_, r) => (
        <div key={r} className={cn('flex gap-4 px-4 py-3', r % 2 === 0 ? 'bg-white' : 'bg-gray-50')}>
          {Array.from({ length: cols }).map((_, c) => (
            <Skeleton key={c} className={cn('h-4 flex-1', c === 0 ? 'max-w-[120px]' : '')} />
          ))}
        </div>
      ))}
    </div>
  );
}
```

### src/components/ui/Pagination.tsx

```typescript
import { Button } from './Button';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  hasMore:     boolean;
  pageToken?:  string | null;
  onNext:      () => void;
  onPrev?:     () => void;
  canGoPrev?:  boolean;
  totalEstimate?: number;
  pageSize:    number;
  currentCount:number;
}

export function Pagination({
  hasMore, onNext, onPrev, canGoPrev, totalEstimate, currentCount
}: PaginationProps) {
  return (
    <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100">
      <span className="text-sm text-gray-500">
        Showing <span className="font-medium text-gray-700">{currentCount}</span> events
        {totalEstimate ? (
          <> · ~<span className="font-medium text-gray-700">{totalEstimate.toLocaleString('en-IN')}</span> total</>
        ) : null}
      </span>
      <div className="flex items-center gap-2">
        <Button
          variant="secondary" size="sm"
          icon={<ChevronLeft className="h-4 w-4" />}
          onClick={onPrev}
          disabled={!canGoPrev}
        >
          Prev
        </Button>
        <Button
          variant="secondary" size="sm"
          iconRight={<ChevronRight className="h-4 w-4" />}
          onClick={onNext}
          disabled={!hasMore}
        >
          Next
        </Button>
      </div>
    </div>
  );
}
```

### src/components/ui/DateRangePicker.tsx

```typescript
import { useState } from 'react';
import { DayPicker, DateRange } from 'react-day-picker';
import { Popover, Transition } from '@headlessui/react';
import { CalendarDays, ChevronDown } from 'lucide-react';
import { format, subDays, startOfDay, endOfDay } from 'date-fns';
import { Button } from './Button';
import 'react-day-picker/dist/style.css';

interface DateRangePickerProps {
  startTime: string;
  endTime:   string;
  onChange:  (start: string, end: string) => void;
}

const PRESETS = [
  { label: 'Last 1 hour',   fn: () => ({ start: new Date(Date.now() - 3600_000), end: new Date() }) },
  { label: 'Last 24 hours', fn: () => ({ start: subDays(new Date(), 1), end: new Date() }) },
  { label: 'Last 7 days',   fn: () => ({ start: subDays(new Date(), 7), end: new Date() }) },
  { label: 'Last 30 days',  fn: () => ({ start: subDays(new Date(), 30), end: new Date() }) },
  { label: 'This month',    fn: () => { const n = new Date(); return { start: new Date(n.getFullYear(), n.getMonth(), 1), end: n }; }},
];

export function DateRangePicker({ startTime, endTime, onChange }: DateRangePickerProps) {
  const [range, setRange] = useState<DateRange | undefined>({
    from: new Date(startTime),
    to:   new Date(endTime),
  });

  const label = range?.from && range?.to
    ? `${format(range.from, 'dd MMM')} – ${format(range.to, 'dd MMM yyyy')}`
    : 'Select range';

  const apply = (from: Date, to: Date) => {
    onChange(startOfDay(from).toISOString(), endOfDay(to).toISOString());
  };

  return (
    <Popover className="relative">
      <Popover.Button as={Button} variant="secondary" size="sm"
        icon={<CalendarDays className="h-4 w-4 text-gray-500" />}
        iconRight={<ChevronDown className="h-3 w-3 text-gray-400" />}
      >
        {label}
      </Popover.Button>

      <Transition
        enter="transition duration-100 ease-out"
        enterFrom="opacity-0 scale-95" enterTo="opacity-100 scale-100"
        leave="transition duration-75 ease-in"
        leaveFrom="opacity-100 scale-100" leaveTo="opacity-0 scale-95"
      >
        <Popover.Panel className="absolute right-0 z-50 mt-2 rounded-xl border border-gray-200 bg-white shadow-panel">
          {({ close }) => (
            <div className="flex">
              {/* Presets */}
              <div className="w-36 border-r border-gray-100 p-3 space-y-1">
                {PRESETS.map((p) => (
                  <button
                    key={p.label}
                    onClick={() => {
                      const { start, end } = p.fn();
                      setRange({ from: start, to: end });
                      apply(start, end);
                      close();
                    }}
                    className="w-full text-left px-2 py-1.5 text-xs text-gray-600 rounded hover:bg-blue-50 hover:text-blue-700 transition-colors"
                  >
                    {p.label}
                  </button>
                ))}
              </div>
              {/* Calendar */}
              <div className="p-3">
                <DayPicker
                  mode="range" selected={range} onSelect={setRange}
                  numberOfMonths={2}
                  disabled={{ after: new Date() }}
                  classNames={{
                    day_selected:     'bg-blue-600 text-white',
                    day_range_middle: 'bg-blue-50 text-blue-900',
                  }}
                />
                <div className="flex justify-end gap-2 pt-2 border-t border-gray-100">
                  <Button variant="ghost" size="sm" onClick={() => close()}>Cancel</Button>
                  <Button size="sm" onClick={() => {
                    if (range?.from && range?.to) { apply(range.from, range.to); close(); }
                  }}>
                    Apply
                  </Button>
                </div>
              </div>
            </div>
          )}
        </Popover.Panel>
      </Transition>
    </Popover>
  );
}
```

### src/components/ui/Dropdown.tsx

```typescript
import { Fragment } from 'react';
import { Menu, Transition } from '@headlessui/react';
import { cn } from '@/utils/formatting.utils';

interface DropdownItem {
  label:     string;
  onClick?:  () => void;
  icon?:     React.ReactNode;
  danger?:   boolean;
  disabled?: boolean;
  divider?:  boolean;
}

interface DropdownProps {
  trigger:  React.ReactNode;
  items:    DropdownItem[];
  align?:   'left' | 'right';
}

export function Dropdown({ trigger, items, align = 'right' }: DropdownProps) {
  return (
    <Menu as="div" className="relative inline-block text-left">
      <Menu.Button as={Fragment}>{trigger}</Menu.Button>
      <Transition
        enter="transition duration-100 ease-out"
        enterFrom="opacity-0 scale-95" enterTo="opacity-100 scale-100"
        leave="transition duration-75 ease-in"
        leaveFrom="opacity-100 scale-100" leaveTo="opacity-0 scale-95"
      >
        <Menu.Items
          className={cn(
            'absolute z-50 mt-1 w-48 rounded-lg bg-white shadow-panel border border-gray-100',
            'focus:outline-none divide-y divide-gray-50',
            align === 'right' ? 'right-0' : 'left-0'
          )}
        >
          {items.map((item, i) =>
            item.divider ? (
              <div key={i} className="h-px bg-gray-100 my-1" />
            ) : (
              <Menu.Item key={i} disabled={item.disabled}>
                {({ active }) => (
                  <button
                    onClick={item.onClick}
                    className={cn(
                      'flex w-full items-center gap-2 px-3 py-2 text-sm transition-colors',
                      active && !item.danger && 'bg-gray-50 text-gray-900',
                      active && item.danger  && 'bg-red-50 text-red-700',
                      !active && item.danger && 'text-red-600',
                      !active && !item.danger && 'text-gray-700',
                      item.disabled && 'opacity-50 cursor-not-allowed'
                    )}
                  >
                    {item.icon && <span className="h-4 w-4 flex-shrink-0">{item.icon}</span>}
                    {item.label}
                  </button>
                )}
              </Menu.Item>
            )
          )}
        </Menu.Items>
      </Transition>
    </Menu>
  );
}
```

### src/components/ui/EmptyState.tsx

```typescript
import { cn } from '@/utils/formatting.utils';
import { Button } from './Button';

interface EmptyStateProps {
  icon:       React.ReactNode;
  title:      string;
  description:string;
  action?:    { label: string; onClick: () => void };
  className?: string;
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div className={cn('flex flex-col items-center justify-center py-16 text-center', className)}>
      <div className="mb-4 rounded-full bg-gray-100 p-4 text-gray-400">{icon}</div>
      <h3 className="mb-1 text-base font-semibold text-gray-900">{title}</h3>
      <p className="mb-4 text-sm text-gray-500 max-w-xs">{description}</p>
      {action && <Button variant="primary" size="sm" onClick={action.onClick}>{action.label}</Button>}
    </div>
  );
}
```

---

## 15. Layout Components

### src/components/layout/AppShell.tsx

```typescript
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { useTenant } from '@/hooks/useTenant';

export function AppShell() {
  useTenant(); // Initialize applications on mount

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <TopBar />
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
```

### src/components/layout/Sidebar.tsx

```typescript
import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, ClipboardList, FileText, Bell, RotateCcw,
  Building, AppWindow, Key, Users, CreditCard, Shield, Database,
  ChevronDown, ShieldCheck
} from 'lucide-react';
import { useState } from 'react';
import { cn } from '@/utils/formatting.utils';
import { useAuth } from '@/hooks/useAuth';

interface NavItem {
  label: string;
  icon:  React.ReactNode;
  href:  string;
  roles?: string[];
  badge?: string;
}

const MAIN_NAV: NavItem[] = [
  { label: 'Dashboard',    icon: <LayoutDashboard className="h-4 w-4" />, href: '/dashboard' },
  { label: 'Audit Events', icon: <ClipboardList className="h-4 w-4" />,  href: '/events' },
  { label: 'Reports',      icon: <FileText className="h-4 w-4" />,       href: '/reports',   roles: ['OWNER','ADMIN','AUDITOR'] },
  { label: 'Alerts',       icon: <Bell className="h-4 w-4" />,           href: '/alerts',    roles: ['OWNER','ADMIN','AUDITOR'] },
  { label: 'Replay',       icon: <RotateCcw className="h-4 w-4" />,      href: '/replay',    roles: ['OWNER','ADMIN'] },
];

const SETTINGS_NAV: NavItem[] = [
  { label: 'Organization',  icon: <Building className="h-4 w-4" />,  href: '/settings/organization',  roles: ['OWNER','ADMIN'] },
  { label: 'Applications',  icon: <AppWindow className="h-4 w-4" />, href: '/settings/applications',  roles: ['OWNER','ADMIN'] },
  { label: 'API Keys',      icon: <Key className="h-4 w-4" />,       href: '/settings/api-keys' },
  { label: 'Team',          icon: <Users className="h-4 w-4" />,     href: '/settings/team',          roles: ['OWNER','ADMIN'] },
  { label: 'Billing',       icon: <CreditCard className="h-4 w-4" />,href: '/settings/billing',       roles: ['OWNER'] },
  { label: 'Retention',     icon: <Database className="h-4 w-4" />,  href: '/settings/retention',     roles: ['OWNER','ADMIN'] },
];

export function Sidebar() {
  const { user, hasRole } = useAuth();
  const location = useLocation();
  const [settingsOpen, setSettingsOpen] = useState(
    location.pathname.startsWith('/settings')
  );

  const isVisible = (item: NavItem) =>
    !item.roles || hasRole(...item.roles);

  return (
    <aside className="flex h-full w-56 flex-col bg-sidebar-bg border-r border-sidebar-border">
      {/* Logo */}
      <div className="flex h-14 items-center gap-2.5 px-4 border-b border-sidebar-border">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-600">
          <ShieldCheck className="h-4 w-4 text-white" />
        </div>
        <span className="text-sm font-bold text-white tracking-tight">AuditHub</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-0.5">
        {MAIN_NAV.filter(isVisible).map((item) => (
          <SidebarLink key={item.href} item={item} />
        ))}

        {/* Settings collapsible */}
        {SETTINGS_NAV.some(isVisible) && (
          <div className="pt-4">
            <button
              onClick={() => setSettingsOpen(!settingsOpen)}
              className="flex w-full items-center justify-between px-2 py-1 text-xs font-semibold uppercase tracking-wider text-gray-500 hover:text-gray-400"
            >
              Settings
              <ChevronDown className={cn('h-3 w-3 transition-transform', settingsOpen && 'rotate-180')} />
            </button>
            {settingsOpen && (
              <div className="mt-1 space-y-0.5">
                {SETTINGS_NAV.filter(isVisible).map((item) => (
                  <SidebarLink key={item.href} item={item} />
                ))}
              </div>
            )}
          </div>
        )}
      </nav>

      {/* User footer */}
      <div className="border-t border-sidebar-border px-3 py-3">
        <div className="flex items-center gap-2.5 rounded-lg px-2 py-2">
          <div className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-blue-700 text-xs font-bold text-white">
            {user?.name?.charAt(0).toUpperCase()}
          </div>
          <div className="flex-1 min-w-0">
            <p className="truncate text-xs font-medium text-gray-200">{user?.name}</p>
            <p className="truncate text-2xs text-gray-500">{user?.role}</p>
          </div>
        </div>
      </div>
    </aside>
  );
}

function SidebarLink({ item }: { item: NavItem }) {
  return (
    <NavLink
      to={item.href}
      className={({ isActive }) =>
        cn(
          'flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-medium transition-all duration-150',
          isActive
            ? 'bg-blue-700/80 text-white shadow-sm'
            : 'text-gray-400 hover:bg-sidebar-hover hover:text-gray-100'
        )
      }
    >
      {item.icon}
      {item.label}
      {item.badge && (
        <span className="ml-auto rounded-full bg-red-500 px-1.5 py-0.5 text-2xs font-bold text-white">
          {item.badge}
        </span>
      )}
    </NavLink>
  );
}
```

### src/components/layout/TopBar.tsx

```typescript
import { Bell, LogOut, Search, User } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { OrgSwitcher } from './OrgSwitcher';
import { Dropdown } from '@/components/ui/Dropdown';
import { useNavigate } from 'react-router-dom';

export function TopBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="flex h-14 items-center justify-between border-b border-gray-200 bg-white px-6">
      {/* Left: App switcher */}
      <OrgSwitcher />

      {/* Right: actions */}
      <div className="flex items-center gap-2">
        <button
          onClick={() => navigate('/events')}
          className="flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-1.5 text-sm text-gray-500 hover:bg-gray-100 transition-colors"
        >
          <Search className="h-3.5 w-3.5" />
          Search events…
          <kbd className="ml-2 rounded border border-gray-200 bg-white px-1 py-0.5 text-2xs font-mono text-gray-400">/</kbd>
        </button>

        <button className="relative rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700 transition-colors">
          <Bell className="h-4 w-4" />
          <span className="absolute top-1.5 right-1.5 h-1.5 w-1.5 rounded-full bg-red-500" />
        </button>

        <Dropdown
          trigger={
            <button className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-blue-700 text-sm font-bold hover:bg-blue-200 transition-colors">
              {user?.name?.charAt(0).toUpperCase()}
            </button>
          }
          items={[
            {
              label: 'Your Profile',
              icon: <User className="h-4 w-4" />,
              onClick: () => navigate('/settings/organization'),
            },
            { divider: true } as { divider: true; label: '' },
            {
              label: 'Sign Out',
              icon: <LogOut className="h-4 w-4" />,
              onClick: logout,
              danger: true,
            },
          ]}
        />
      </div>
    </header>
  );
}
```

### src/components/layout/OrgSwitcher.tsx

```typescript
import { useTenant } from '@/hooks/useTenant';
import { Dropdown } from '@/components/ui/Dropdown';
import { ChevronDown } from 'lucide-react';

export function OrgSwitcher() {
  const { selectedApp, applications, setSelectedApp } = useTenant();

  if (!applications.length) return null;

  return (
    <Dropdown
      align="left"
      trigger={
        <button className="flex items-center gap-2 rounded-lg border border-gray-200 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors">
          <span className="h-2 w-2 rounded-full bg-green-500" />
          {selectedApp?.name ?? 'All Applications'}
          <ChevronDown className="h-3.5 w-3.5 text-gray-400" />
        </button>
      }
      items={[
        {
          label: 'All Applications',
          onClick: () => setSelectedApp(null),
        },
        ...applications.map((app) => ({
          label: app.name,
          onClick: () => setSelectedApp(app.id),
        })),
      ]}
    />
  );
}
```

---

## 16. Audit Components

### src/components/audit/FilterBar.tsx

```typescript
import { useState } from 'react';
import { SlidersHorizontal, X, Search } from 'lucide-react';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { DateRangePicker } from '@/components/ui/DateRangePicker';
import type { EventFilters, ActionType, Severity, Outcome } from '@/types/audit.types';
import { cn } from '@/utils/formatting.utils';

interface FilterBarProps {
  filters:  EventFilters;
  onChange: (f: EventFilters) => void;
}

const ACTION_TYPES: ActionType[] = ['CREATE','UPDATE','DELETE','READ','LOGIN','LOGOUT','EXPORT','APPROVE','REJECT','TRANSFER'];
const SEVERITIES:   Severity[]   = ['LOW','MEDIUM','HIGH','CRITICAL'];
const OUTCOMES:     Outcome[]    = ['SUCCESS','FAILURE','PARTIAL'];

function activeFilterCount(f: EventFilters): number {
  let count = 0;
  if (f.actionTypes?.length)  count++;
  if (f.severities?.length)   count++;
  if (f.outcomes?.length)     count++;
  if (f.resourceType)         count++;
  if (f.actorUserId)          count++;
  if (f.tags?.length)         count++;
  return count;
}

export function FilterBar({ filters, onChange }: FilterBarProps) {
  const [expanded, setExpanded] = useState(false);
  const active = activeFilterCount(filters);

  const toggle = <T extends string>(arr: T[] | undefined, val: T): T[] => {
    const a = arr ?? [];
    return a.includes(val) ? a.filter((v) => v !== val) : [...a, val];
  };

  const clear = () =>
    onChange({ startTime: filters.startTime, endTime: filters.endTime });

  return (
    <div className="space-y-3">
      {/* Primary row */}
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex-1 min-w-64">
          <Input
            placeholder="Search events…"
            value={filters.query ?? ''}
            onChange={(e) => onChange({ ...filters, query: e.target.value })}
            icon={<Search className="h-4 w-4" />}
          />
        </div>
        <DateRangePicker
          startTime={filters.startTime}
          endTime={filters.endTime}
          onChange={(s, e) => onChange({ ...filters, startTime: s, endTime: e })}
        />
        <Button
          variant="secondary" size="sm"
          icon={<SlidersHorizontal className="h-4 w-4" />}
          onClick={() => setExpanded(!expanded)}
        >
          Filters
          {active > 0 && (
            <span className="ml-1 flex h-4 w-4 items-center justify-center rounded-full bg-blue-600 text-2xs font-bold text-white">
              {active}
            </span>
          )}
        </Button>
        {active > 0 && (
          <Button variant="ghost" size="sm" icon={<X className="h-3 w-3" />} onClick={clear}>
            Clear
          </Button>
        )}
      </div>

      {/* Expanded filters */}
      {expanded && (
        <div className="rounded-lg border border-gray-200 bg-gray-50 p-3 space-y-3 animate-slide-in-up">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {/* Action types */}
            <div>
              <p className="text-xs font-medium text-gray-500 mb-2">Action Type</p>
              <div className="flex flex-wrap gap-1">
                {ACTION_TYPES.map((t) => (
                  <button key={t} onClick={() => onChange({ ...filters, actionTypes: toggle(filters.actionTypes, t) })}
                    className={cn('rounded px-2 py-0.5 text-xs font-medium border transition-colors',
                      filters.actionTypes?.includes(t)
                        ? 'border-blue-500 bg-blue-500 text-white'
                        : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300 hover:bg-gray-50'
                    )}>
                    {t}
                  </button>
                ))}
              </div>
            </div>

            {/* Severity */}
            <div>
              <p className="text-xs font-medium text-gray-500 mb-2">Severity</p>
              <div className="flex flex-wrap gap-1">
                {SEVERITIES.map((s) => (
                  <button key={s} onClick={() => onChange({ ...filters, severities: toggle(filters.severities, s) })}
                    className={cn('rounded px-2 py-0.5 text-xs font-medium border transition-colors',
                      filters.severities?.includes(s)
                        ? 'border-blue-500 bg-blue-500 text-white'
                        : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                    )}>
                    {s}
                  </button>
                ))}
              </div>
            </div>

            {/* Outcome */}
            <div>
              <p className="text-xs font-medium text-gray-500 mb-2">Outcome</p>
              <div className="flex flex-wrap gap-1">
                {OUTCOMES.map((o) => (
                  <button key={o} onClick={() => onChange({ ...filters, outcomes: toggle(filters.outcomes, o) })}
                    className={cn('rounded px-2 py-0.5 text-xs font-medium border transition-colors',
                      filters.outcomes?.includes(o)
                        ? 'border-blue-500 bg-blue-500 text-white'
                        : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                    )}>
                    {o}
                  </button>
                ))}
              </div>
            </div>

            {/* Resource type */}
            <div>
              <p className="text-xs font-medium text-gray-500 mb-2">Resource Type</p>
              <Input
                placeholder="e.g. LoanApplication"
                value={filters.resourceType ?? ''}
                onChange={(e) => onChange({ ...filters, resourceType: e.target.value || undefined })}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
```

### src/components/audit/AuditEventTable.tsx

```typescript
import { Table, TableHeader, TableBody, Th, Td } from '@/components/ui/Table';
import { SeverityBadge, OutcomeBadge } from '@/components/ui/Badge';
import type { AuditEvent } from '@/types/audit.types';
import { formatIST, formatRelative } from '@/utils/date.utils';
import { cn } from '@/utils/formatting.utils';
import { ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';

interface AuditEventTableProps {
  events:          AuditEvent[];
  selectedEventId?:string;
  onEventClick:    (event: AuditEvent) => void;
}

export function AuditEventTable({ events, selectedEventId, onEventClick }: AuditEventTableProps) {
  return (
    <Table>
      <TableHeader>
        <tr>
          <Th>Time</Th>
          <Th>Actor</Th>
          <Th>Action</Th>
          <Th>Resource</Th>
          <Th>Severity</Th>
          <Th>Outcome</Th>
          <Th />
        </tr>
      </TableHeader>
      <TableBody>
        {events.map((event) => (
          <AuditEventRow
            key={event.eventId}
            event={event}
            isSelected={event.eventId === selectedEventId}
            onClick={() => onEventClick(event)}
          />
        ))}
      </TableBody>
    </Table>
  );
}

function AuditEventRow({ event, isSelected, onClick }: {
  event: AuditEvent; isSelected: boolean; onClick: () => void;
}) {
  return (
    <tr
      onClick={onClick}
      className={cn(
        'cursor-pointer transition-colors',
        isSelected ? 'bg-blue-50' : 'hover:bg-gray-50'
      )}
    >
      <Td className="whitespace-nowrap">
        <div>
          <p className="font-medium text-gray-800 text-xs">{formatIST(event.eventTime)}</p>
          <p className="text-2xs text-gray-400 mt-0.5">{formatRelative(event.eventTime)}</p>
        </div>
      </Td>
      <Td>
        <div>
          <p className="font-medium text-gray-800 text-xs">{event.actor.userName ?? event.actor.userId}</p>
          <p className="text-2xs text-gray-400 mt-0.5">{event.actor.userEmail}</p>
        </div>
      </Td>
      <Td>
        <div>
          <p className="font-medium text-gray-800 text-xs font-mono">{event.action.name}</p>
          <p className="text-2xs text-gray-400 mt-0.5">{event.action.type}</p>
        </div>
      </Td>
      <Td>
        <div>
          <Link
            to={`/entities/${event.resource.type}/${event.resource.id}`}
            onClick={(e) => e.stopPropagation()}
            className="text-xs font-medium text-blue-600 hover:text-blue-700 flex items-center gap-1"
          >
            {event.resource.type}
            <ExternalLink className="h-2.5 w-2.5" />
          </Link>
          <p className="text-2xs text-gray-400 font-mono mt-0.5">{event.resource.id}</p>
        </div>
      </Td>
      <Td><SeverityBadge severity={event.severity} /></Td>
      <Td><OutcomeBadge outcome={event.outcome} /></Td>
      <Td>
        {event.changes.length > 0 && (
          <span className="text-2xs text-gray-400">{event.changes.length} change{event.changes.length !== 1 ? 's' : ''}</span>
        )}
      </Td>
    </tr>
  );
}
```

### src/components/audit/AuditEventDetail.tsx

```typescript
import { X, Copy, ExternalLink, Code } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { AuditEvent } from '@/types/audit.types';
import { SeverityBadge, OutcomeBadge, Badge } from '@/components/ui/Badge';
import { DiffViewer } from './DiffViewer';
import { formatIST } from '@/utils/date.utils';
import { copyToClipboard, truncate } from '@/utils/formatting.utils';
import toast from 'react-hot-toast';
import { useState } from 'react';

interface AuditEventDetailProps {
  event:   AuditEvent;
  onClose: () => void;
}

export function AuditEventDetail({ event, onClose }: AuditEventDetailProps) {
  const [showRaw, setShowRaw] = useState(false);

  const copy = () => {
    copyToClipboard(JSON.stringify(event, null, 2));
    toast.success('Copied to clipboard');
  };

  return (
    <div className="h-full flex flex-col bg-white animate-slide-in-right">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3 sticky top-0 bg-white z-10">
        <div className="flex items-center gap-2 min-w-0">
          <SeverityBadge severity={event.severity} />
          <span className="truncate text-sm font-medium text-gray-900">{event.action.name}</span>
        </div>
        <button onClick={onClose} className="ml-2 rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors flex-shrink-0">
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto p-4 space-y-5">

        {/* Meta */}
        <Section title="Event Details">
          <MetaRow label="Event ID">
            <code className="text-2xs font-mono text-gray-700 bg-gray-50 px-1.5 py-0.5 rounded">{truncate(event.eventId, 36)}</code>
          </MetaRow>
          <MetaRow label="Time">{formatIST(event.eventTime)} IST</MetaRow>
          <MetaRow label="Outcome"><OutcomeBadge outcome={event.outcome} /></MetaRow>
          <MetaRow label="Action Type"><Badge variant="gray">{event.action.type}</Badge></MetaRow>
          {event.correlationId && (
            <MetaRow label="Correlation">
              <code className="text-2xs font-mono text-gray-600">{event.correlationId}</code>
            </MetaRow>
          )}
        </Section>

        {/* Actor */}
        <Section title="Actor">
          <div className="flex items-center gap-3 mb-3">
            <div className="h-9 w-9 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
              <span className="text-sm font-bold text-blue-700">
                {(event.actor.userName ?? event.actor.userId).charAt(0).toUpperCase()}
              </span>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-900">{event.actor.userName ?? event.actor.userId}</p>
              <p className="text-xs text-gray-500">{event.actor.userEmail}</p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <SmallMetaRow label="User ID">{event.actor.userId}</SmallMetaRow>
            {event.actor.ipAddress && <SmallMetaRow label="IP">{event.actor.ipAddress}</SmallMetaRow>}
          </div>
          <Link
            to={`/users/${event.actor.userId}/activity`}
            className="mt-3 inline-flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700"
          >
            View all activity <ExternalLink className="h-3 w-3" />
          </Link>
        </Section>

        {/* Resource */}
        <Section title="Resource">
          <MetaRow label="Type"><Badge variant="blue">{event.resource.type}</Badge></MetaRow>
          <MetaRow label="ID"><code className="text-2xs font-mono text-gray-700">{event.resource.id}</code></MetaRow>
          {event.resource.name && <MetaRow label="Name">{event.resource.name}</MetaRow>}
          <Link
            to={`/entities/${event.resource.type}/${event.resource.id}`}
            className="mt-2 inline-flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700"
          >
            View entity history <ExternalLink className="h-3 w-3" />
          </Link>
        </Section>

        {/* Changes */}
        {event.changes.length > 0 && (
          <Section title={`Changes (${event.changes.length})`}>
            <DiffViewer changes={event.changes} />
          </Section>
        )}

        {/* Metadata */}
        {event.metadata && Object.keys(event.metadata).length > 0 && (
          <Section title="Metadata">
            <div className="rounded-lg bg-gray-50 p-3 space-y-1.5">
              {Object.entries(event.metadata).map(([k, v]) => (
                <div key={k} className="flex justify-between text-xs">
                  <span className="font-mono text-gray-500">{k}</span>
                  <span className="text-gray-700 ml-4 text-right max-w-40 truncate">{v}</span>
                </div>
              ))}
            </div>
          </Section>
        )}

        {/* Tags */}
        {event.tags && event.tags.length > 0 && (
          <Section title="Tags">
            <div className="flex flex-wrap gap-1.5">
              {event.tags.map((tag) => (
                <Badge key={tag} variant="blue" size="sm">#{tag}</Badge>
              ))}
            </div>
          </Section>
        )}

        {/* Raw JSON */}
        {showRaw && (
          <Section title="Raw Payload">
            <pre className="text-2xs font-mono bg-gray-900 text-green-400 rounded-lg p-3 overflow-x-auto">
              {JSON.stringify(event, null, 2)}
            </pre>
          </Section>
        )}
      </div>

      {/* Footer */}
      <div className="border-t border-gray-100 px-4 py-3">
        <div className="flex gap-2">
          <button onClick={copy}
            className="flex-1 flex items-center justify-center gap-1.5 rounded-lg border border-gray-200 py-2 text-xs font-medium text-gray-600 hover:bg-gray-50 transition-colors">
            <Copy className="h-3.5 w-3.5" /> Copy JSON
          </button>
          <button onClick={() => setShowRaw(!showRaw)}
            className="flex-1 flex items-center justify-center gap-1.5 rounded-lg border border-gray-200 py-2 text-xs font-medium text-gray-600 hover:bg-gray-50 transition-colors">
            <Code className="h-3.5 w-3.5" /> {showRaw ? 'Hide' : 'Raw'}
          </button>
        </div>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h3 className="mb-2 text-xs font-semibold uppercase tracking-wider text-gray-400">{title}</h3>
      {children}
    </div>
  );
}

function MetaRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between py-1 text-xs">
      <span className="text-gray-500 flex-shrink-0 w-28">{label}</span>
      <span className="text-gray-900 text-right">{children}</span>
    </div>
  );
}

function SmallMetaRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="bg-gray-50 rounded p-2">
      <p className="text-2xs text-gray-400 mb-0.5">{label}</p>
      <p className="text-xs font-mono text-gray-700 truncate">{children as string}</p>
    </div>
  );
}
```

### src/components/audit/DiffViewer.tsx

```typescript
import type { FieldChange } from '@/types/audit.types';

interface DiffViewerProps {
  changes: FieldChange[];
}

export function DiffViewer({ changes }: DiffViewerProps) {
  return (
    <div className="rounded-lg border border-gray-200 overflow-hidden divide-y divide-gray-100">
      {changes.map((change, i) => (
        <div key={i}>
          <div className="bg-gray-50 px-3 py-1.5 flex items-center gap-2">
            <span className="text-xs font-mono font-semibold text-gray-600">{change.fieldName}</span>
          </div>
          <div className="grid grid-cols-2 divide-x divide-gray-100">
            <div className="bg-red-50 px-3 py-2">
              <p className="text-2xs font-bold text-red-400 mb-1">BEFORE</p>
              <code className="text-xs text-red-700 break-all block">
                {change.oldValue != null ? JSON.stringify(change.oldValue) : <span className="text-gray-400 italic">null</span>}
              </code>
            </div>
            <div className="bg-green-50 px-3 py-2">
              <p className="text-2xs font-bold text-green-400 mb-1">AFTER</p>
              <code className="text-xs text-green-700 break-all block">
                {change.newValue != null ? JSON.stringify(change.newValue) : <span className="text-gray-400 italic">null</span>}
              </code>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
```

### src/components/audit/EntityTimeline.tsx

```typescript
import type { AuditEvent } from '@/types/audit.types';
import { SeverityBadge, OutcomeBadge } from '@/components/ui/Badge';
import { DiffViewer } from './DiffViewer';
import { formatIST } from '@/utils/date.utils';
import { useState } from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '@/utils/formatting.utils';

interface EntityTimelineProps {
  events: AuditEvent[];
}

export function EntityTimeline({ events }: EntityTimelineProps) {
  return (
    <div className="relative">
      {/* Vertical line */}
      <div className="absolute left-4 top-4 bottom-4 w-px bg-gray-200" />
      <div className="space-y-4">
        {events.map((event, i) => (
          <TimelineEvent key={event.eventId} event={event} isLast={i === events.length - 1} />
        ))}
      </div>
    </div>
  );
}

function TimelineEvent({ event, isLast }: { event: AuditEvent; isLast: boolean }) {
  const [open, setOpen] = useState(false);
  const hasChanges = event.changes.length > 0;

  return (
    <div className="flex gap-4 pl-0">
      {/* Dot */}
      <div className={cn(
        'relative z-10 flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full border-2 border-white',
        event.severity === 'CRITICAL' ? 'bg-purple-100 ring-2 ring-purple-300' :
        event.severity === 'HIGH'     ? 'bg-red-100' :
        event.severity === 'MEDIUM'   ? 'bg-amber-100' : 'bg-green-100'
      )}>
        <span className="text-xs font-bold text-gray-600">
          {event.action.type.charAt(0)}
        </span>
      </div>

      {/* Content */}
      <div className="flex-1 min-w-0 pb-4">
        <div
          className={cn(
            'rounded-lg border border-gray-100 bg-white shadow-card',
            hasChanges && 'cursor-pointer hover:border-gray-200 transition-colors'
          )}
          onClick={() => hasChanges && setOpen(!open)}
        >
          <div className="flex items-center justify-between p-3">
            <div className="min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <span className="text-sm font-medium text-gray-900">{event.action.name}</span>
                <SeverityBadge severity={event.severity} />
                <OutcomeBadge outcome={event.outcome} />
              </div>
              <div className="text-xs text-gray-500">
                <span className="font-medium text-gray-700">{event.actor.userName ?? event.actor.userId}</span>
                {' · '}
                {formatIST(event.eventTime)}
              </div>
            </div>
            {hasChanges && (
              <ChevronDown className={cn('h-4 w-4 text-gray-400 flex-shrink-0 transition-transform', open && 'rotate-180')} />
            )}
          </div>

          {open && hasChanges && (
            <div className="border-t border-gray-100 p-3">
              <DiffViewer changes={event.changes} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
```

---

## 17. Dashboard Components

### src/components/dashboard/StatsCards.tsx

```typescript
import { TrendingUp, AlertTriangle, XCircle, Layers } from 'lucide-react';
import type { DashboardStats } from '@/types/audit.types';
import { formatNumber } from '@/utils/formatting.utils';
import { SkeletonCard } from '@/components/ui/Skeleton';

interface StatsCardsProps {
  stats?:     DashboardStats;
  isLoading?: boolean;
}

export function StatsCards({ stats, isLoading }: StatsCardsProps) {
  if (isLoading) return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      {Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)}
    </div>
  );
  if (!stats) return null;

  const cards = [
    {
      label: 'Events Today',
      value: formatNumber(stats.totalEventsToday),
      sub:   `${formatNumber(stats.totalEventsThisMonth)} this month`,
      icon:  <TrendingUp className="h-5 w-5 text-blue-600" />,
      bg:    'bg-blue-50',
    },
    {
      label: 'Critical Events',
      value: formatNumber(stats.criticalEventsToday),
      sub:   'today',
      icon:  <AlertTriangle className="h-5 w-5 text-purple-600" />,
      bg:    'bg-purple-50',
      warn:  stats.criticalEventsToday > 0,
    },
    {
      label: 'Failed Events',
      value: formatNumber(stats.failedEventsToday),
      sub:   'today',
      icon:  <XCircle className="h-5 w-5 text-red-500" />,
      bg:    'bg-red-50',
      warn:  stats.failedEventsToday > 5,
    },
    {
      label: 'Quota Used',
      value: `${stats.quotaUsedPercent.toFixed(1)}%`,
      sub:   `${formatNumber(stats.totalEventsThisMonth)} / ${formatNumber(stats.monthlyQuota)}`,
      icon:  <Layers className="h-5 w-5 text-emerald-600" />,
      bg:    'bg-emerald-50',
      warn:  stats.quotaUsedPercent > 80,
    },
  ];

  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      {cards.map((card) => (
        <div key={card.label}
          className="rounded-xl border border-gray-100 bg-white p-5 shadow-card hover:shadow-card-hover transition-shadow">
          <div className="flex items-center justify-between mb-3">
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{card.label}</p>
            <div className={`rounded-lg p-1.5 ${card.bg}`}>{card.icon}</div>
          </div>
          <p className={`text-2xl font-bold ${card.warn ? 'text-red-600' : 'text-gray-900'}`}>{card.value}</p>
          <p className="text-xs text-gray-500 mt-1">{card.sub}</p>
        </div>
      ))}
    </div>
  );
}
```

### src/components/dashboard/EventTrendChart.tsx

```typescript
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import type { TrendPoint } from '@/types/audit.types';
import { formatShort } from '@/utils/date.utils';

interface EventTrendChartProps {
  data: TrendPoint[];
}

export function EventTrendChart({ data }: EventTrendChartProps) {
  const formatted = data.map((d) => ({
    ...d,
    date: formatShort(d.date + 'T00:00:00Z'),
  }));

  return (
    <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-card">
      <div className="mb-4">
        <h3 className="text-sm font-semibold text-gray-900">Event Volume</h3>
        <p className="text-xs text-gray-500 mt-0.5">Last 30 days</p>
      </div>
      <ResponsiveContainer width="100%" height={200}>
        <AreaChart data={formatted} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
          <defs>
            <linearGradient id="colorTotal" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%"  stopColor="#3b82f6" stopOpacity={0.15} />
              <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
            </linearGradient>
            <linearGradient id="colorCritical" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%"  stopColor="#7c3aed" stopOpacity={0.15} />
              <stop offset="95%" stopColor="#7c3aed" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
          <XAxis dataKey="date" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} />
          <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} />
          <Tooltip
            contentStyle={{ background: '#1e293b', border: 'none', borderRadius: 8, fontSize: 12, color: '#f1f5f9' }}
            cursor={{ stroke: '#3b82f6', strokeWidth: 1, strokeDasharray: '4 4' }}
          />
          <Legend wrapperStyle={{ fontSize: 11, paddingTop: 8 }} />
          <Area type="monotone" dataKey="count" name="Total" stroke="#3b82f6" fill="url(#colorTotal)" strokeWidth={2} dot={false} />
          <Area type="monotone" dataKey="criticalCount" name="Critical" stroke="#7c3aed" fill="url(#colorCritical)" strokeWidth={1.5} dot={false} />
          <Area type="monotone" dataKey="failureCount" name="Failures" stroke="#ef4444" fill="none" strokeWidth={1.5} strokeDasharray="4 4" dot={false} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
```

### src/components/dashboard/SeverityDonut.tsx

```typescript
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import type { DashboardStats } from '@/types/audit.types';

const COLORS = { LOW: '#22c55e', MEDIUM: '#f59e0b', HIGH: '#ef4444', CRITICAL: '#7c3aed' };

export function SeverityDonut({ stats }: { stats?: DashboardStats }) {
  if (!stats) return null;

  const data = [
    { name: 'Low',      value: Math.max(1, stats.totalEventsToday - stats.criticalEventsToday - stats.failedEventsToday) },
    { name: 'Critical', value: stats.criticalEventsToday },
    { name: 'Failures', value: stats.failedEventsToday },
  ].filter((d) => d.value > 0);

  return (
    <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-card h-full">
      <h3 className="text-sm font-semibold text-gray-900 mb-4">Today's Events</h3>
      <ResponsiveContainer width="100%" height={180}>
        <PieChart>
          <Pie data={data} cx="50%" cy="50%" innerRadius={50} outerRadius={75}
            paddingAngle={3} dataKey="value">
            {data.map((entry) => (
              <Cell key={entry.name} fill={COLORS[entry.name as keyof typeof COLORS] ?? '#94a3b8'} />
            ))}
          </Pie>
          <Tooltip contentStyle={{ background: '#1e293b', border: 'none', borderRadius: 8, fontSize: 12, color: '#f1f5f9' }} />
          <Legend wrapperStyle={{ fontSize: 11 }} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
```

### src/components/dashboard/TopActorsTable.tsx

```typescript
import type { TopActor } from '@/types/audit.types';
import { Link } from 'react-router-dom';
import { formatNumber } from '@/utils/formatting.utils';

export function TopActorsTable({ actors }: { actors: TopActor[] }) {
  return (
    <div className="rounded-xl border border-gray-100 bg-white shadow-card">
      <div className="border-b border-gray-100 px-5 py-3">
        <h3 className="text-sm font-semibold text-gray-900">Top Active Users</h3>
        <p className="text-xs text-gray-500 mt-0.5">This month</p>
      </div>
      <div className="divide-y divide-gray-50">
        {actors.map((actor, i) => (
          <div key={actor.userId} className="flex items-center gap-3 px-5 py-3">
            <span className="w-5 text-xs font-bold text-gray-400 text-center">{i + 1}</span>
            <div className="h-7 w-7 flex-shrink-0 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center">
              <span className="text-xs font-bold text-white">
                {actor.userName?.charAt(0)?.toUpperCase()}
              </span>
            </div>
            <div className="flex-1 min-w-0">
              <Link to={`/users/${actor.userId}/activity`}
                className="text-sm font-medium text-gray-900 hover:text-blue-600 transition-colors truncate block">
                {actor.userName}
              </Link>
              <p className="text-xs text-gray-400 font-mono truncate">{actor.userId}</p>
            </div>
            <div className="text-right">
              <p className="text-sm font-semibold text-gray-900">{formatNumber(actor.eventCount)}</p>
              <p className="text-2xs text-gray-400">events</p>
            </div>
          </div>
        ))}
        {actors.length === 0 && (
          <p className="text-sm text-gray-400 text-center py-8">No data yet</p>
        )}
      </div>
    </div>
  );
}
```

### src/components/dashboard/QuotaUsageBar.tsx

```typescript
import { useNavigate } from 'react-router-dom';
import { formatNumber } from '@/utils/formatting.utils';
import { cn } from '@/utils/formatting.utils';

interface QuotaUsageBarProps {
  used:  number;
  limit: number;
}

export function QuotaUsageBar({ used, limit }: QuotaUsageBarProps) {
  const pct = Math.min((used / limit) * 100, 100);
  const navigate = useNavigate();

  const color = pct >= 90 ? 'bg-red-500' : pct >= 80 ? 'bg-amber-500' : 'bg-blue-500';
  const label = pct >= 90 ? 'text-red-600' : pct >= 80 ? 'text-amber-600' : 'text-blue-600';

  return (
    <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-card">
      <div className="flex items-center justify-between mb-3">
        <div>
          <h3 className="text-sm font-semibold text-gray-900">Monthly Quota</h3>
          <p className="text-xs text-gray-500 mt-0.5">Resets on 1st of next month</p>
        </div>
        <span className={cn('text-xl font-bold', label)}>{pct.toFixed(1)}%</span>
      </div>
      <div className="h-2.5 w-full rounded-full bg-gray-100 overflow-hidden">
        <div className={cn('h-full rounded-full transition-all duration-500', color)} style={{ width: `${pct}%` }} />
      </div>
      <div className="flex justify-between mt-2">
        <span className="text-xs text-gray-500">{formatNumber(used)} used</span>
        <span className="text-xs text-gray-500">{formatNumber(limit)} limit</span>
      </div>
      {pct >= 80 && (
        <button onClick={() => navigate('/settings/billing')}
          className="mt-3 w-full rounded-lg border border-amber-200 bg-amber-50 py-2 text-xs font-medium text-amber-700 hover:bg-amber-100 transition-colors">
          Upgrade Plan →
        </button>
      )}
    </div>
  );
}
```

### src/components/dashboard/ActivityHeatmap.tsx

```typescript
import type { TrendPoint } from '@/types/audit.types';
import { parseISO, getDay, format } from 'date-fns';
import { cn } from '@/utils/formatting.utils';

export function ActivityHeatmap({ data }: { data: TrendPoint[] }) {
  const max = Math.max(...data.map((d) => d.count), 1);

  const getIntensity = (count: number) => {
    const ratio = count / max;
    if (ratio === 0)     return 'bg-gray-100';
    if (ratio < 0.25)    return 'bg-blue-100';
    if (ratio < 0.5)     return 'bg-blue-300';
    if (ratio < 0.75)    return 'bg-blue-500';
    return 'bg-blue-700';
  };

  return (
    <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-card">
      <h3 className="text-sm font-semibold text-gray-900 mb-4">Activity Heatmap</h3>
      <div className="flex flex-wrap gap-1">
        {data.map((point) => (
          <div
            key={point.date}
            title={`${format(parseISO(point.date), 'dd MMM')}: ${point.count} events`}
            className={cn('h-4 w-4 rounded cursor-default transition-colors', getIntensity(point.count))}
          />
        ))}
      </div>
      <div className="flex items-center gap-1 mt-3">
        <span className="text-2xs text-gray-400 mr-1">Less</span>
        {['bg-gray-100','bg-blue-100','bg-blue-300','bg-blue-500','bg-blue-700'].map((c) => (
          <div key={c} className={cn('h-3 w-3 rounded', c)} />
        ))}
        <span className="text-2xs text-gray-400 ml-1">More</span>
      </div>
    </div>
  );
}
```

---

## 18. Report Components

### src/components/reports/ReportList.tsx

```typescript
import { FileText, Download, Clock, CheckCircle, XCircle, Loader } from 'lucide-react';
import type { GeneratedReport } from '@/types/api.types';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { formatRelative } from '@/utils/date.utils';
import { formatBytes, formatNumber } from '@/utils/formatting.utils';

const STATUS_ICONS = {
  PENDING:    <Clock className="h-4 w-4 text-amber-500" />,
  PROCESSING: <Loader className="h-4 w-4 text-blue-500 animate-spin" />,
  COMPLETED:  <CheckCircle className="h-4 w-4 text-green-500" />,
  FAILED:     <XCircle className="h-4 w-4 text-red-500" />,
};

export function ReportList({ reports, onRefresh }: { reports: GeneratedReport[]; onRefresh: () => void }) {
  if (!reports.length) return (
    <div className="text-center py-16 text-gray-400">
      <FileText className="h-10 w-10 mx-auto mb-3 opacity-50" />
      <p className="text-sm font-medium">No reports yet</p>
      <p className="text-xs mt-1">Generate your first compliance report above</p>
    </div>
  );

  return (
    <div className="divide-y divide-gray-100">
      {reports.map((report) => (
        <div key={report.id} className="flex items-center gap-4 py-4 px-1">
          <div className="flex-shrink-0">{STATUS_ICONS[report.status]}</div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-900 truncate">{report.name}</p>
            <div className="flex items-center gap-3 mt-0.5">
              <span className="text-xs text-gray-400">{formatRelative(report.createdAt)}</span>
              <Badge variant="gray" size="sm">{report.format}</Badge>
              {report.rowCount && <span className="text-xs text-gray-400">{formatNumber(report.rowCount)} rows</span>}
              {report.fileSizeBytes && <span className="text-xs text-gray-400">{formatBytes(report.fileSizeBytes)}</span>}
            </div>
            {report.errorMessage && <p className="text-xs text-red-600 mt-1">{report.errorMessage}</p>}
          </div>
          <div className="flex-shrink-0">
            {report.status === 'COMPLETED' && report.downloadUrl ? (
              <Button
                variant="secondary" size="xs"
                icon={<Download className="h-3 w-3" />}
                onClick={() => window.open(report.downloadUrl, '_blank')}
              >
                Download
              </Button>
            ) : report.status === 'PROCESSING' || report.status === 'PENDING' ? (
              <Button variant="ghost" size="xs" onClick={onRefresh}>Refresh</Button>
            ) : null}
          </div>
        </div>
      ))}
    </div>
  );
}
```

### src/components/reports/GenerateReportModal.tsx

```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { reportsApi } from '@/api/reports.api';
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
```

### src/components/reports/ReportStatusBadge.tsx

```typescript
import type { GeneratedReport } from '@/types/api.types';
import { Badge } from '@/components/ui/Badge';

export function ReportStatusBadge({ status }: { status: GeneratedReport['status'] }) {
  const map = {
    PENDING:    { variant: 'amber' as const,  label: 'Pending' },
    PROCESSING: { variant: 'blue' as const,   label: 'Processing' },
    COMPLETED:  { variant: 'green' as const,  label: 'Completed' },
    FAILED:     { variant: 'red' as const,    label: 'Failed' },
  };
  const cfg = map[status];
  return <Badge variant={cfg.variant}>{cfg.label}</Badge>;
}
```

---

## 19. Pages — Auth

### src/pages/auth/LoginPage.tsx

```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { ShieldCheck, Eye, EyeOff } from 'lucide-react';
import { useState } from 'react';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/hooks/useAuth';

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
                <button type="button" onClick={() => setShowPwd(!showPwd)} className="text-gray-400 hover:text-gray-600">
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
```

### src/pages/auth/SamlCallbackPage.tsx

```typescript
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/store/auth.store';
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
```

---

## 20. Pages — Onboarding

### src/pages/onboarding/SignupPage.tsx

```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { authApi } from '@/api/auth.api';
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
```

### src/pages/onboarding/OnboardingWizard.tsx

```typescript
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, Copy, Terminal, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { copyToClipboard } from '@/utils/formatting.utils';
import toast from 'react-hot-toast';
import { cn } from '@/utils/formatting.utils';

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
                className="absolute top-2 right-2 rounded p-1.5 text-gray-500 hover:bg-gray-700 hover:text-gray-300">
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
```

---

## 21. Pages — Main App

### src/pages/DashboardPage.tsx

```typescript
import { AlertTriangle } from 'lucide-react';
import { useDashboardStats } from '@/hooks/useDashboardStats';
import { StatsCards } from '@/components/dashboard/StatsCards';
import { EventTrendChart } from '@/components/dashboard/EventTrendChart';
import { SeverityDonut } from '@/components/dashboard/SeverityDonut';
import { TopActorsTable } from '@/components/dashboard/TopActorsTable';
import { QuotaUsageBar } from '@/components/dashboard/QuotaUsageBar';
import { ActivityHeatmap } from '@/components/dashboard/ActivityHeatmap';
import { OrgSwitcher } from '@/components/layout/OrgSwitcher';

export function DashboardPage() {
  const { stats, trends } = useDashboardStats();

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-sm text-gray-500 mt-0.5">Real-time audit trail overview</p>
        </div>
      </div>

      {/* Quota warning */}
      {stats.data && stats.data.quotaUsedPercent > 80 && (
        <div className="flex items-center gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3">
          <AlertTriangle className="h-4 w-4 text-amber-600 flex-shrink-0" />
          <span className="text-sm text-amber-800">
            You've used <strong>{stats.data.quotaUsedPercent.toFixed(0)}%</strong> of your monthly event quota.{' '}
            <a href="/settings/billing" className="font-semibold underline">Upgrade now →</a>
          </span>
        </div>
      )}

      {/* Stat cards */}
      <StatsCards stats={stats.data} isLoading={stats.isLoading} />

      {/* Charts */}
      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-12 lg:col-span-8">
          <EventTrendChart data={trends.data?.dataPoints ?? []} />
        </div>
        <div className="col-span-12 lg:col-span-4">
          <SeverityDonut stats={stats.data} />
        </div>
      </div>

      {/* Bottom row */}
      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-12 lg:col-span-5">
          <TopActorsTable actors={stats.data?.topActors ?? []} />
        </div>
        <div className="col-span-12 lg:col-span-3">
          <QuotaUsageBar
            used={stats.data?.totalEventsThisMonth ?? 0}
            limit={stats.data?.monthlyQuota ?? 10000}
          />
        </div>
        <div className="col-span-12 lg:col-span-4">
          <ActivityHeatmap data={trends.data?.dataPoints ?? []} />
        </div>
      </div>
    </div>
  );
}
```

### src/pages/EventsPage.tsx

```typescript
import { useState, useCallback } from 'react';
import { subDays } from 'date-fns';
import { Download, RefreshCw } from 'lucide-react';
import { FilterBar } from '@/components/audit/FilterBar';
import { AuditEventTable } from '@/components/audit/AuditEventTable';
import { AuditEventDetail } from '@/components/audit/AuditEventDetail';
import { Pagination } from '@/components/ui/Pagination';
import { Button } from '@/components/ui/Button';
import { TableSkeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { useAuditEvents } from '@/hooks/useAuditEvents';
import type { EventFilters, AuditEvent } from '@/types/audit.types';
import { ClipboardList } from 'lucide-react';
import { cn } from '@/utils/formatting.utils';
import { startOfDayISO, endOfDayISO } from '@/utils/date.utils';

const DEFAULT_FILTERS: EventFilters = {
  startTime: startOfDayISO(subDays(new Date(), 7)),
  endTime:   endOfDayISO(new Date()),
};

export function EventsPage() {
  const [filters, setFilters]           = useState<EventFilters>(DEFAULT_FILTERS);
  const [pageToken, setPageToken]       = useState<string | undefined>();
  const [pageHistory, setPageHistory]   = useState<string[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<AuditEvent | null>(null);

  const { data, isLoading, isFetching, refetch } = useAuditEvents(filters, { pageToken });

  const handleFiltersChange = useCallback((f: EventFilters) => {
    setFilters(f);
    setPageToken(undefined);
    setPageHistory([]);
    setSelectedEvent(null);
  }, []);

  const goNext = () => {
    if (data?.pageToken) {
      setPageHistory((h) => [...h, pageToken ?? '']);
      setPageToken(data.pageToken);
    }
  };

  const goPrev = () => {
    const history = [...pageHistory];
    const prev = history.pop();
    setPageHistory(history);
    setPageToken(prev ?? undefined);
  };

  return (
    <div className="flex h-full overflow-hidden">
      {/* Main panel */}
      <div className={cn('flex flex-col flex-1 overflow-hidden', selectedEvent ? 'w-1/2' : 'w-full')}>
        {/* Filter bar */}
        <div className="border-b border-gray-200 bg-white p-4 sticky top-0 z-10">
          <FilterBar filters={filters} onChange={handleFiltersChange} />
        </div>

        {/* Results meta */}
        <div className="flex items-center justify-between border-b border-gray-100 bg-gray-50 px-4 py-2">
          <span className="text-xs text-gray-500">
            {isFetching && !isLoading ? (
              <span className="flex items-center gap-1"><RefreshCw className="h-3 w-3 animate-spin" /> Refreshing…</span>
            ) : (
              <>~<span className="font-medium text-gray-700">{data?.totalEstimate?.toLocaleString('en-IN') ?? 0}</span> events</>
            )}
          </span>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="xs" icon={<RefreshCw className="h-3 w-3" />} onClick={() => refetch()}>Refresh</Button>
            <Button variant="ghost" size="xs" icon={<Download className="h-3 w-3" />}>Export CSV</Button>
          </div>
        </div>

        {/* Table */}
        <div className="flex-1 overflow-auto">
          {isLoading ? <TableSkeleton rows={12} cols={7} /> :
           !data?.content.length ? (
             <EmptyState
               icon={<ClipboardList className="h-8 w-8" />}
               title="No events found"
               description="Try adjusting your filters or time range."
             />
           ) : (
             <AuditEventTable
               events={data.content}
               selectedEventId={selectedEvent?.eventId}
               onEventClick={setSelectedEvent}
             />
           )}
        </div>

        {/* Pagination */}
        {data && (
          <Pagination
            hasMore={data.hasMore}
            pageToken={data.pageToken}
            canGoPrev={pageHistory.length > 0}
            onNext={goNext}
            onPrev={goPrev}
            totalEstimate={data.totalEstimate}
            pageSize={50}
            currentCount={data.content.length}
          />
        )}
      </div>

      {/* Detail panel */}
      {selectedEvent && (
        <div className="w-1/2 border-l border-gray-200 overflow-hidden flex-shrink-0">
          <AuditEventDetail event={selectedEvent} onClose={() => setSelectedEvent(null)} />
        </div>
      )}
    </div>
  );
}
```

### src/pages/EventDetailPage.tsx

```typescript
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { useAuditEvent } from '@/hooks/useAuditEvents';
import { AuditEventDetail } from '@/components/audit/AuditEventDetail';
import { Skeleton } from '@/components/ui/Skeleton';
import { Button } from '@/components/ui/Button';

export function EventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const { data, isLoading } = useAuditEvent(
    eventId!,
    params.get('applicationId') ?? '',
    params.get('eventTime') ?? ''
  );

  if (isLoading) return (
    <div className="p-6 space-y-4">
      <Skeleton className="h-6 w-48" />
      <Skeleton className="h-96 w-full rounded-xl" />
    </div>
  );

  if (!data) return (
    <div className="p-6">
      <p className="text-sm text-gray-500">Event not found.</p>
    </div>
  );

  return (
    <div className="max-w-3xl mx-auto p-6">
      <Button variant="ghost" size="sm" icon={<ArrowLeft className="h-4 w-4" />}
        className="mb-4" onClick={() => navigate(-1)}>Back</Button>
      <div className="rounded-xl border border-gray-200 overflow-hidden">
        <AuditEventDetail event={data} onClose={() => navigate(-1)} />
      </div>
    </div>
  );
}
```

### src/pages/EntityHistoryPage.tsx

```typescript
import { useParams } from 'react-router-dom';
import { useEntityHistory } from '@/hooks/useAuditEvents';
import { EntityTimeline } from '@/components/audit/EntityTimeline';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { Clock } from 'lucide-react';

export function EntityHistoryPage() {
  const { resourceType, resourceId } = useParams<{ resourceType: string; resourceId: string }>();
  const { data, isLoading } = useEntityHistory(resourceType!, resourceId!);

  return (
    <div className="max-w-3xl mx-auto p-6">
      <div className="mb-6">
        <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">{resourceType}</span>
        <h1 className="text-xl font-bold text-gray-900 mt-1 font-mono">{resourceId}</h1>
        <p className="text-sm text-gray-500 mt-0.5">Complete audit history for this entity</p>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-20 w-full rounded-xl" />)}
        </div>
      ) : !data?.content.length ? (
        <EmptyState
          icon={<Clock className="h-8 w-8" />}
          title="No history found"
          description="No audit events have been recorded for this entity."
        />
      ) : (
        <EntityTimeline events={data.content} />
      )}
    </div>
  );
}
```

### src/pages/UserActivityPage.tsx

```typescript
import { useParams } from 'react-router-dom';
import { useState } from 'react';
import { subDays } from 'date-fns';
import { useUserActivity } from '@/hooks/useAuditEvents';
import { AuditEventTable } from '@/components/audit/AuditEventTable';
import { AuditEventDetail } from '@/components/audit/AuditEventDetail';
import { TableSkeleton } from '@/components/ui/Skeleton';
import type { AuditEvent } from '@/types/audit.types';

export function UserActivityPage() {
  const { userId } = useParams<{ userId: string }>();
  const [selected, setSelected] = useState<AuditEvent | null>(null);

  const { data, isLoading } = useUserActivity(userId!, {
    startTime: subDays(new Date(), 30).toISOString(),
    endTime:   new Date().toISOString(),
  });

  return (
    <div className="flex h-full overflow-hidden">
      <div className={`flex flex-col flex-1 overflow-hidden ${selected ? 'w-1/2' : 'w-full'}`}>
        <div className="border-b border-gray-200 bg-white px-6 py-4">
          <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">User Activity</span>
          <h1 className="text-lg font-bold text-gray-900 mt-0.5 font-mono">{userId}</h1>
          <p className="text-xs text-gray-500 mt-0.5">Last 30 days · {data?.content.length ?? 0} events</p>
        </div>
        <div className="flex-1 overflow-auto">
          {isLoading ? <TableSkeleton /> : (
            <AuditEventTable events={data?.content ?? []} selectedEventId={selected?.eventId} onEventClick={setSelected} />
          )}
        </div>
      </div>
      {selected && (
        <div className="w-1/2 border-l border-gray-200 overflow-hidden">
          <AuditEventDetail event={selected} onClose={() => setSelected(null)} />
        </div>
      )}
    </div>
  );
}
```

### src/pages/ReportsPage.tsx

```typescript
import { useState } from 'react';
import { Plus } from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { ReportList } from '@/components/reports/ReportList';
import { GenerateReportModal } from '@/components/reports/GenerateReportModal';
import { reportsApi } from '@/api/reports.api';
import { TableSkeleton } from '@/components/ui/Skeleton';

export function ReportsPage() {
  const [showModal, setShowModal] = useState(false);
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['reports'],
    queryFn:  () => reportsApi.list({ size: 20 }),
    refetchInterval: 10_000, // auto-refresh for in-progress reports
  });

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Compliance Reports</h1>
          <p className="text-sm text-gray-500 mt-0.5">Generate PDF, CSV, or Excel reports for audits</p>
        </div>
        <Button icon={<Plus className="h-4 w-4" />} onClick={() => setShowModal(true)}>
          New Report
        </Button>
      </div>

      <div className="rounded-xl border border-gray-100 bg-white shadow-card px-2">
        {isLoading ? <TableSkeleton rows={5} cols={4} /> : (
          <ReportList
            reports={data?.content ?? []}
            onRefresh={() => qc.invalidateQueries({ queryKey: ['reports'] })}
          />
        )}
      </div>

      <GenerateReportModal open={showModal} onClose={() => setShowModal(false)} />
    </div>
  );
}
```

### src/pages/AlertsPage.tsx

```typescript
import { useState } from 'react';
import { Plus, Bell, ToggleLeft, ToggleRight, Trash2 } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { alertsApi } from '@/api/organizations.api';
import type { AlertRule } from '@/types/audit.types';
import { EmptyState } from '@/components/ui/EmptyState';
import toast from 'react-hot-toast';

export function AlertsPage() {
  const qc = useQueryClient();
  const { data: rules = [], isLoading } = useQuery({ queryKey: ['alert-rules'], queryFn: alertsApi.listRules });

  const toggle = useMutation({
    mutationFn: (rule: AlertRule) => alertsApi.updateRule(rule.id, { isActive: !rule.isActive }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['alert-rules'] }); toast.success('Rule updated'); },
  });

  const remove = useMutation({
    mutationFn: (id: string) => alertsApi.deleteRule(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['alert-rules'] }); toast.success('Rule deleted'); },
  });

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Alert Rules</h1>
          <p className="text-sm text-gray-500 mt-0.5">Get notified when suspicious patterns occur</p>
        </div>
        <Button icon={<Plus className="h-4 w-4" />}>New Rule</Button>
      </div>

      {isLoading ? null : !rules.length ? (
        <EmptyState
          icon={<Bell className="h-8 w-8" />}
          title="No alert rules yet"
          description="Create a rule to get notified when unusual activity is detected."
          action={{ label: 'Create first rule', onClick: () => {} }}
        />
      ) : (
        <div className="space-y-3">
          {rules.map((rule: AlertRule) => (
            <div key={rule.id} className="rounded-xl border border-gray-100 bg-white p-4 shadow-card flex items-center gap-4">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <p className="text-sm font-semibold text-gray-900">{rule.name}</p>
                  <Badge variant={rule.isActive ? 'green' : 'gray'}>{rule.isActive ? 'Active' : 'Paused'}</Badge>
                  <Badge variant={
                    rule.severity === 'CRITICAL' ? 'purple' :
                    rule.severity === 'HIGH' ? 'red' :
                    rule.severity === 'MEDIUM' ? 'amber' : 'green'
                  }>{rule.severity}</Badge>
                </div>
                <p className="text-xs text-gray-500">
                  {rule.conditionConfig.threshold} {rule.conditionConfig.actionType ?? 'any'} events in {rule.conditionConfig.windowMinutes} min
                </p>
                <div className="flex gap-1 mt-2">
                  {rule.notificationChannels.map((ch) => (
                    <Badge key={ch} variant="blue" size="sm">{ch}</Badge>
                  ))}
                </div>
              </div>
              <div className="flex items-center gap-2 flex-shrink-0">
                <button onClick={() => toggle.mutate(rule)} className="text-gray-400 hover:text-blue-600 transition-colors">
                  {rule.isActive ? <ToggleRight className="h-5 w-5 text-blue-600" /> : <ToggleLeft className="h-5 w-5" />}
                </button>
                <button onClick={() => remove.mutate(rule.id)} className="text-gray-400 hover:text-red-600 transition-colors">
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
```

### src/pages/ReplayPage.tsx

```typescript
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { subDays } from 'date-fns';
import { RotateCcw, AlertTriangle } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { replayApi } from '@/api/organizations.api';
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
```

---

## 22. Pages — Settings

### src/pages/settings/OrganizationSettings.tsx

```typescript
import { useForm } from 'react-hook-form';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { organizationApi } from '@/api/auth.api';
import { Skeleton } from '@/components/ui/Skeleton';
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
```

### src/pages/settings/ApplicationsSettings.tsx

```typescript
import { useState } from 'react';
import { Plus, Trash2, ExternalLink } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { applicationsApi } from '@/api/organizations.api';
import type { Application } from '@/types/api.types';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';

export function ApplicationsSettings() {
  const qc = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const { data: apps = [] } = useQuery({ queryKey: ['applications'], queryFn: applicationsApi.list });
  const { register, handleSubmit, reset } = useForm<{ name: string; environment: string; description: string }>();

  const create = useMutation({
    mutationFn: applicationsApi.create,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['applications'] }); toast.success('Application created'); setShowModal(false); reset(); },
  });

  const remove = useMutation({
    mutationFn: applicationsApi.delete,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['applications'] }); toast.success('Application deleted'); },
  });

  const ENV_COLOR: Record<string, 'green' | 'amber' | 'blue'> = { PRODUCTION: 'green', STAGING: 'amber', DEVELOPMENT: 'blue' };

  return (
    <div className="p-6 max-w-3xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold text-gray-900">Applications</h1>
        <Button icon={<Plus className="h-4 w-4" />} onClick={() => setShowModal(true)}>New Application</Button>
      </div>

      <div className="space-y-3">
        {apps.map((app: Application) => (
          <div key={app.id} className="rounded-xl border border-gray-100 bg-white p-4 shadow-card flex items-center gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <p className="font-semibold text-gray-900 text-sm">{app.name}</p>
                <Badge variant={ENV_COLOR[app.environment] ?? 'gray'}>{app.environment}</Badge>
              </div>
              <p className="text-xs text-gray-400 font-mono">{app.id}</p>
              {app.description && <p className="text-xs text-gray-500 mt-1">{app.description}</p>}
            </div>
            <button onClick={() => remove.mutate(app.id)} className="text-gray-300 hover:text-red-500 transition-colors">
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        ))}
      </div>

      <Modal open={showModal} onClose={() => setShowModal(false)} title="New Application" size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
            <Button loading={create.isPending} onClick={handleSubmit((d) => create.mutate(d))}>Create</Button>
          </>
        }
      >
        <form className="space-y-4">
          <Input label="Application Name" placeholder="NetBanking Portal" {...register('name')} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Environment</label>
            <select {...register('environment')} className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20">
              <option value="PRODUCTION">Production</option>
              <option value="STAGING">Staging</option>
              <option value="DEVELOPMENT">Development</option>
            </select>
          </div>
          <Input label="Description (optional)" placeholder="Customer-facing internet banking" {...register('description')} />
        </form>
      </Modal>
    </div>
  );
}
```

### src/pages/settings/ApiKeysSettings.tsx

```typescript
import { useState } from 'react';
import { Plus, Eye, EyeOff, Copy, Trash2, Key } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { apiKeysApi } from '@/api/organizations.api';
import type { ApiKey, ApiKeyCreated } from '@/types/api.types';
import { useForm } from 'react-hook-form';
import { copyToClipboard } from '@/utils/formatting.utils';
import { formatRelative } from '@/utils/date.utils';
import toast from 'react-hot-toast';

export function ApiKeysSettings() {
  const qc = useQueryClient();
  const [showModal, setShowModal]         = useState(false);
  const [newKey, setNewKey]               = useState<ApiKeyCreated | null>(null);
  const [showNewKey, setShowNewKey]       = useState(false);
  const { data: keys = [] }               = useQuery({ queryKey: ['api-keys'], queryFn: apiKeysApi.list });
  const { register, handleSubmit, reset } = useForm<{ name: string; keyType: string }>();

  const create = useMutation({
    mutationFn: apiKeysApi.create,
    onSuccess: (data: ApiKeyCreated) => {
      qc.invalidateQueries({ queryKey: ['api-keys'] });
      setNewKey(data);
      setShowModal(false);
      reset();
    },
  });

  const revoke = useMutation({
    mutationFn: apiKeysApi.revoke,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['api-keys'] }); toast.success('Key revoked'); },
  });

  return (
    <div className="p-6 max-w-3xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">API Keys</h1>
          <p className="text-sm text-gray-500 mt-0.5">Keys are shown only once. Store them in your secrets manager.</p>
        </div>
        <Button icon={<Plus className="h-4 w-4" />} onClick={() => setShowModal(true)}>New Key</Button>
      </div>

      {/* New key banner */}
      {newKey && (
        <div className="rounded-xl border border-green-200 bg-green-50 p-4 mb-6">
          <p className="text-sm font-semibold text-green-900 mb-2 flex items-center gap-2">
            <Key className="h-4 w-4" /> Your new API key — copy it now, it won't be shown again
          </p>
          <div className="flex items-center gap-2">
            <code className="flex-1 rounded-lg bg-gray-900 px-3 py-2 text-xs font-mono text-green-400 break-all">
              {showNewKey ? newKey.plainTextKey : '•'.repeat(newKey.plainTextKey.length)}
            </code>
            <button onClick={() => setShowNewKey(!showNewKey)} className="text-gray-400 hover:text-gray-600">
              {showNewKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
            <button onClick={() => { copyToClipboard(newKey.plainTextKey); toast.success('Copied'); }}
              className="text-gray-400 hover:text-gray-600">
              <Copy className="h-4 w-4" />
            </button>
          </div>
          <button onClick={() => setNewKey(null)} className="text-xs text-green-700 mt-2 hover:underline">Dismiss</button>
        </div>
      )}

      {/* Key list */}
      <div className="space-y-3">
        {keys.map((key: ApiKey) => (
          <div key={key.id} className="rounded-xl border border-gray-100 bg-white p-4 shadow-card flex items-center gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <p className="text-sm font-semibold text-gray-900">{key.name}</p>
                <Badge variant={key.keyType === 'ADMIN' ? 'red' : key.keyType === 'WRITE' ? 'amber' : 'green'}>
                  {key.keyType}
                </Badge>
                {!key.isActive && <Badge variant="gray">Revoked</Badge>}
              </div>
              <p className="text-xs font-mono text-gray-400">{key.keyPrefix}••••••••••••••••••</p>
              <p className="text-xs text-gray-400 mt-1">
                {key.lastUsedAt ? `Last used ${formatRelative(key.lastUsedAt)}` : 'Never used'}
              </p>
            </div>
            {key.isActive && (
              <button onClick={() => revoke.mutate(key.id)} className="text-gray-300 hover:text-red-500 transition-colors">
                <Trash2 className="h-4 w-4" />
              </button>
            )}
          </div>
        ))}
      </div>

      <Modal open={showModal} onClose={() => setShowModal(false)} title="Create API Key" size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
            <Button loading={create.isPending} onClick={handleSubmit((d) => create.mutate(d))}>Create</Button>
          </>
        }
      >
        <form className="space-y-4">
          <Input label="Key Name" placeholder="e.g. NetBanking Write Key" {...register('name')} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Key Type</label>
            <select {...register('keyType')} className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none">
              <option value="WRITE">WRITE — Ingest events only</option>
              <option value="READ">READ — Query events only</option>
              <option value="ADMIN">ADMIN — Full access</option>
            </select>
          </div>
        </form>
      </Modal>
    </div>
  );
}
```

### src/pages/settings/TeamSettings.tsx

```typescript
import { useState } from 'react';
import { Plus, Trash2, UserPlus } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { teamApi } from '@/api/organizations.api';
import type { UserWithRole } from '@/types/auth.types';
import { useForm } from 'react-hook-form';
import { useAuth } from '@/hooks/useAuth';
import { formatRelative } from '@/utils/date.utils';
import toast from 'react-hot-toast';

const ROLE_COLORS: Record<string, 'purple' | 'red' | 'blue' | 'green' | 'gray'> = {
  OWNER: 'purple', ADMIN: 'red', AUDITOR: 'blue', VIEWER: 'green', DEVELOPER: 'gray'
};

export function TeamSettings() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [showInvite, setShowInvite] = useState(false);
  const { data: users = [] } = useQuery({ queryKey: ['team-users'], queryFn: teamApi.listUsers });
  const { register, handleSubmit, reset } = useForm<{ email: string; role: string }>();

  const invite = useMutation({
    mutationFn: teamApi.invite,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['team-users'] }); toast.success('Invite sent'); setShowInvite(false); reset(); },
  });

  const remove = useMutation({
    mutationFn: teamApi.removeUser,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['team-users'] }); toast.success('User removed'); },
  });

  return (
    <div className="p-6 max-w-3xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Team</h1>
          <p className="text-sm text-gray-500 mt-0.5">{users.length} members</p>
        </div>
        <Button icon={<UserPlus className="h-4 w-4" />} onClick={() => setShowInvite(true)}>Invite Member</Button>
      </div>

      <div className="rounded-xl border border-gray-100 bg-white shadow-card divide-y divide-gray-50">
        {users.map((u: UserWithRole) => (
          <div key={u.id} className="flex items-center gap-4 p-4">
            <div className="h-9 w-9 flex-shrink-0 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center">
              <span className="text-sm font-bold text-white">{u.name.charAt(0).toUpperCase()}</span>
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <p className="text-sm font-medium text-gray-900">{u.name}</p>
                {u.id === user?.id && <Badge variant="gray" size="sm">You</Badge>}
              </div>
              <p className="text-xs text-gray-500">{u.email}</p>
              {u.lastLoginAt && <p className="text-xs text-gray-400 mt-0.5">Last login {formatRelative(u.lastLoginAt)}</p>}
            </div>
            <Badge variant={ROLE_COLORS[u.role] ?? 'gray'}>{u.role}</Badge>
            {u.id !== user?.id && (
              <button onClick={() => remove.mutate(u.id)} className="text-gray-300 hover:text-red-500 transition-colors ml-2">
                <Trash2 className="h-4 w-4" />
              </button>
            )}
          </div>
        ))}
      </div>

      <Modal open={showInvite} onClose={() => setShowInvite(false)} title="Invite Team Member" size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowInvite(false)}>Cancel</Button>
            <Button loading={invite.isPending} onClick={handleSubmit((d) => invite.mutate(d))}>Send Invite</Button>
          </>
        }
      >
        <form className="space-y-4">
          <Input label="Email Address" type="email" placeholder="colleague@company.com" {...register('email')} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Role</label>
            <select {...register('role')} className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none">
              <option value="ADMIN">Admin — Full management access</option>
              <option value="AUDITOR">Auditor — Can view + export events</option>
              <option value="VIEWER">Viewer — Read-only access</option>
              <option value="DEVELOPER">Developer — API key management</option>
            </select>
          </div>
        </form>
      </Modal>
    </div>
  );
}
```

### src/pages/settings/BillingSettings.tsx

```typescript
import { useQuery } from '@tanstack/react-query';
import { organizationApi } from '@/api/auth.api';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { formatNumber, formatBytes } from '@/utils/formatting.utils';
import { CreditCard, ArrowUpRight } from 'lucide-react';

const PLANS = [
  { name: 'Free',         price: '₹0',     events: '10K', retention: '7 days',   apps: 1 },
  { name: 'Starter',      price: '₹999',   events: '1M',  retention: '90 days',  apps: 5 },
  { name: 'Professional', price: '₹4,999', events: '10M', retention: '1 year',   apps: 20 },
  { name: 'Enterprise',   price: 'Custom', events: '∞',   retention: '7 years',  apps: '∞' },
];

export function BillingSettings() {
  const { data: usage, isLoading } = useQuery({ queryKey: ['org-usage'], queryFn: organizationApi.getUsage });
  const { data: org }              = useQuery({ queryKey: ['org-me'],    queryFn: organizationApi.getCurrent });

  return (
    <div className="p-6 max-w-3xl">
      <h1 className="text-xl font-bold text-gray-900 mb-6">Billing & Usage</h1>

      {/* Current usage */}
      <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-card mb-6">
        <h2 className="text-sm font-semibold text-gray-900 mb-4">Current Usage</h2>
        {isLoading ? <Skeleton className="h-20 w-full" /> : (
          <div className="space-y-4">
            <div>
              <div className="flex justify-between text-xs text-gray-500 mb-1.5">
                <span>Events this month</span>
                <span className="font-medium text-gray-700">
                  {formatNumber(usage?.eventsUsed ?? 0)} / {formatNumber(usage?.eventsLimit ?? 0)}
                </span>
              </div>
              <div className="h-2 w-full rounded-full bg-gray-100 overflow-hidden">
                <div className="h-full rounded-full bg-blue-500 transition-all"
                  style={{ width: `${Math.min(usage?.usagePercent ?? 0, 100)}%` }} />
              </div>
            </div>
            {usage?.storageUsedBytes && (
              <div className="flex justify-between text-sm">
                <span className="text-gray-500">Storage used</span>
                <span className="font-medium text-gray-700">{formatBytes(usage.storageUsedBytes)}</span>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Plans */}
      <h2 className="text-sm font-semibold text-gray-900 mb-3">Choose a Plan</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {PLANS.map((plan) => {
          const isCurrent = org?.plan === plan.name.toUpperCase();
          return (
            <div key={plan.name} className={`rounded-xl border p-4 ${isCurrent ? 'border-blue-500 bg-blue-50' : 'border-gray-100 bg-white shadow-card'}`}>
              <div className="flex items-center justify-between mb-3">
                <p className="font-bold text-gray-900">{plan.name}</p>
                {isCurrent && <span className="text-2xs font-semibold text-blue-600 bg-blue-100 rounded-full px-2 py-0.5">Current</span>}
              </div>
              <p className="text-2xl font-bold text-gray-900 mb-3">{plan.price}<span className="text-sm font-normal text-gray-500">/mo</span></p>
              <div className="space-y-1 text-xs text-gray-600 mb-4">
                <p>✓ {plan.events} events/month</p>
                <p>✓ {plan.retention} retention</p>
                <p>✓ {plan.apps} application{plan.apps !== 1 ? 's' : ''}</p>
              </div>
              {!isCurrent && (
                <Button variant={plan.name === 'Enterprise' ? 'secondary' : 'primary'} size="sm" className="w-full"
                  icon={plan.name === 'Enterprise' ? undefined : <ArrowUpRight className="h-3.5 w-3.5" />}>
                  {plan.name === 'Enterprise' ? 'Contact Sales' : 'Upgrade'}
                </Button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
```

### src/pages/settings/RetentionSettings.tsx

```typescript
import { useQuery } from '@tanstack/react-query';
import { organizationApi } from '@/api/auth.api';
import { Badge } from '@/components/ui/Badge';
import { Database } from 'lucide-react';

export function RetentionSettings() {
  const { data: org } = useQuery({ queryKey: ['org-me'], queryFn: organizationApi.getCurrent });

  const retentionDays  = org?.retentionDays ?? 7;
  const retentionLabel = retentionDays >= 365 ? `${Math.floor(retentionDays / 365)} year${Math.floor(retentionDays/365)>1?'s':''}` : `${retentionDays} days`;

  return (
    <div className="p-6 max-w-2xl">
      <h1 className="text-xl font-bold text-gray-900 mb-6">Data Retention</h1>

      <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-card space-y-5">
        <div className="flex items-center gap-4 p-4 rounded-lg bg-blue-50">
          <Database className="h-8 w-8 text-blue-600 flex-shrink-0" />
          <div>
            <p className="text-sm font-semibold text-blue-900">Current Retention Period</p>
            <p className="text-2xl font-bold text-blue-700 mt-0.5">{retentionLabel}</p>
            <p className="text-xs text-blue-600 mt-0.5">Events older than {retentionLabel} are automatically deleted</p>
          </div>
        </div>

        <div className="space-y-3 text-sm text-gray-700">
          <p className="font-medium text-gray-900">Retention by plan:</p>
          {[
            ['FREE',         '7 days',  'gray'],
            ['STARTER',      '90 days', 'blue'],
            ['PROFESSIONAL', '1 year',  'blue'],
            ['ENTERPRISE',   '7 years', 'purple'],
          ].map(([plan, days, color]) => (
            <div key={plan} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
              <Badge variant={color as 'gray' | 'blue' | 'purple'}>{plan}</Badge>
              <span className="text-sm font-medium text-gray-700">{days}</span>
            </div>
          ))}
        </div>

        <p className="text-xs text-gray-400">
          Upgrade your plan to increase retention. For custom periods or DPDP/RBI compliance requirements, contact Enterprise sales.
        </p>
      </div>
    </div>
  );
}
```

---

## 23. src/index.css

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  *, *::before, *::after { box-sizing: border-box; }

  :root {
    --scrollbar-width: 6px;
    --scrollbar-color: #e2e8f0;
  }

  html {
    font-family: 'Inter', system-ui, -apple-system, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    color-scheme: light;
  }

  /* Custom scrollbar */
  ::-webkit-scrollbar { width: var(--scrollbar-width); height: var(--scrollbar-width); }
  ::-webkit-scrollbar-track  { background: transparent; }
  ::-webkit-scrollbar-thumb  { background: var(--scrollbar-color); border-radius: 999px; }
  ::-webkit-scrollbar-thumb:hover { background: #cbd5e1; }

  /* React Day Picker overrides */
  .rdp { --rdp-accent-color: #2563eb; --rdp-background-color: #eff6ff; font-size: 0.8rem; }
  .rdp-day_selected:not(.rdp-day_range_middle) { background-color: var(--rdp-accent-color) !important; }

  /* Remove browser default number input spinners */
  input[type='number'] { -moz-appearance: textfield; }
  input[type='number']::-webkit-outer-spin-button,
  input[type='number']::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
}

@layer utilities {
  /* Hide scrollbar but allow scrolling */
  .scrollbar-hide::-webkit-scrollbar { display: none; }
  .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }

  /* Truncate to 2 lines */
  .line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
```

---

## 24. Environment Variables

### .env.development

```bash
VITE_API_BASE_URL=http://localhost:8080/v1
VITE_APP_ENV=development
VITE_WS_URL=ws://localhost:8080/ws
```

### .env.production

```bash
VITE_API_BASE_URL=https://api.audithub.in/v1
VITE_APP_ENV=production
VITE_WS_URL=wss://api.audithub.in/ws
```

### .env.example

```bash
VITE_API_BASE_URL=http://localhost:8080/v1
VITE_APP_ENV=development
VITE_WS_URL=ws://localhost:8080/ws
```

---

## 25. Additional Config Files

### .eslintrc.cjs

```javascript
module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
  ],
  ignorePatterns: ['dist', '.eslintrc.cjs'],
  parser: '@typescript-eslint/parser',
  rules: {
    '@typescript-eslint/no-explicit-any': 'warn',
    '@typescript-eslint/no-unused-vars':  ['warn', { argsIgnorePattern: '^_' }],
    'react-hooks/exhaustive-deps':        'warn',
  },
};
```

### .prettierrc

```json
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100,
  "bracketSpacing": true,
  "arrowParens": "always"
}
```

### tsconfig.node.json

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

### postcss.config.js

```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

### public/favicon.svg

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" fill="none">
  <rect width="32" height="32" rx="8" fill="#2563eb"/>
  <path d="M16 6L8 10v8c0 5 3.5 9.5 8 11 4.5-1.5 8-6 8-11v-8L16 6z"
        fill="white" fill-opacity="0.9"/>
  <path d="M13 16l2 2 4-4" stroke="#2563eb" stroke-width="2"
        stroke-linecap="round" stroke-linejoin="round"/>
</svg>
```

---

## Quick Start Summary

```bash
# 1. Clone / create project
npm create vite@latest audithub-frontend -- --template react-ts
cd audithub-frontend

# 2. Install dependencies (one command)
npm install react-router-dom @tanstack/react-query @tanstack/react-query-devtools \
  axios zustand recharts date-fns react-hot-toast lucide-react clsx tailwind-merge \
  @headlessui/react react-hook-form @hookform/resolvers zod react-day-picker \
  @stomp/stompjs

npm install -D tailwindcss postcss autoprefixer @types/node @tailwindcss/forms \
  eslint @typescript-eslint/parser @typescript-eslint/eslint-plugin \
  eslint-plugin-react-hooks prettier

# 3. Init Tailwind
npx tailwindcss init -p

# 4. Copy all files from this document

# 5. Set env vars
cp .env.example .env.development

# 6. Start dev server
npm run dev
# → http://localhost:3000

# 7. Build for production
npm run build
```

---

*frontend.md — AuditHub Complete Frontend Reference*  
*Version 1.0 | June 2025 | React 18 + TypeScript + Tailwind CSS*
