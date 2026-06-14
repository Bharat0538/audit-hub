import { cn } from '../../utils/formatting.utils';
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
    <div className={cn('flex flex-col items-center justify-center py-16 text-center w-full', className)}>
      <div className="mb-4 rounded-full bg-gray-100 p-4 text-gray-400">{icon}</div>
      <h3 className="mb-1 text-base font-semibold text-gray-900">{title}</h3>
      <p className="mb-4 text-sm text-gray-500 max-w-xs">{description}</p>
      {action && <Button variant="primary" size="sm" onClick={action.onClick}>{action.label}</Button>}
    </div>
  );
}
