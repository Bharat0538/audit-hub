import type { GeneratedReport } from '../../types';
import { Badge } from '../ui/Badge';

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
