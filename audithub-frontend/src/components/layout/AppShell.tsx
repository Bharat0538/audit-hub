import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { useTenant } from '../../hooks/useTenant';

export function AppShell() {
  useTenant(); // Initialize applications on mount

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50 w-full">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <TopBar />
        <main className="flex-1 overflow-y-auto bg-gray-50">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
