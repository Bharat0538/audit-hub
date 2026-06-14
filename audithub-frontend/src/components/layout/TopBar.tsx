import { Bell, LogOut, Search, User } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { OrgSwitcher } from './OrgSwitcher';
import { Dropdown } from '../ui/Dropdown';
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
