import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/auth.store';
import { AppShell } from './components/layout/AppShell';

// Auth pages
import { LoginPage } from './pages/auth/LoginPage';
import { SamlCallbackPage } from './pages/auth/SamlCallbackPage';

// Onboarding
import { SignupPage } from './pages/onboarding/SignupPage';
import { OnboardingWizard } from './pages/onboarding/OnboardingWizard';

// App pages
import { DashboardPage } from './pages/DashboardPage';
import { EventsPage } from './pages/EventsPage';
import { EventDetailPage } from './pages/EventDetailPage';
import { EntityHistoryPage } from './pages/EntityHistoryPage';
import { UserActivityPage } from './pages/UserActivityPage';
import { ReportsPage } from './pages/ReportsPage';
import { AlertsPage } from './pages/AlertsPage';
import { ReplayPage } from './pages/ReplayPage';
import { TestEventsPage } from './pages/TestEventsPage';

// Settings pages
import { OrganizationSettings } from './pages/settings/OrganizationSettings';
import { ApplicationsSettings } from './pages/settings/ApplicationsSettings';
import { ApiKeysSettings } from './pages/settings/ApiKeysSettings';
import { TeamSettings } from './pages/settings/TeamSettings';
import { BillingSettings } from './pages/settings/BillingSettings';
import { RetentionSettings } from './pages/settings/RetentionSettings';

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
          <Route path="test-events" element={<TestEventsPage />} />
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
