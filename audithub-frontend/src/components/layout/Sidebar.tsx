import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, ClipboardList, FileText, Bell, RotateCcw,
  Building, AppWindow, Key, Users, CreditCard, Database,
  ChevronDown, ShieldCheck, Beaker
} from 'lucide-react';

import { useState } from 'react';
import { cn } from '../../utils/formatting.utils';
import { useAuth } from '../../hooks/useAuth';

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
  { label: 'Test Events',  icon: <Beaker className="h-4 w-4" />,         href: '/test-events', roles: ['OWNER','ADMIN'], badge: 'DEV' },
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
    <aside className="flex h-full w-56 flex-col bg-sidebar-bg border-r border-sidebar-border select-none">
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
              className="flex w-full items-center justify-between px-2 py-1 text-xs font-semibold uppercase tracking-wider text-gray-500 hover:text-gray-400 cursor-pointer"
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
