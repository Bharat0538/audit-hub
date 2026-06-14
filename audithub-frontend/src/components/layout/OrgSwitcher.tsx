import { useTenant } from '../../hooks/useTenant';
import { Dropdown } from '../ui/Dropdown';
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
