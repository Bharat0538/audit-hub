import type { Severity, Outcome } from '../types';

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
