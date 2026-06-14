import { cn } from '../../utils/formatting.utils';
import type { Severity, Outcome } from '../../types';
import { getSeverityConfig, OUTCOME_CONFIG } from '../../utils/severity.utils';

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
      {cfg?.label ?? outcome}
    </span>
  );
}
