import { FileText, Download, Clock, CheckCircle, XCircle, Loader } from 'lucide-react';
import type { GeneratedReport } from '../../types';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { formatRelative } from '../../utils/date.utils';
import { formatBytes, formatNumber } from '../../utils/formatting.utils';

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
              {report.rowCount !== undefined && <span className="text-xs text-gray-400">{formatNumber(report.rowCount)} rows</span>}
              {report.fileSizeBytes !== undefined && <span className="text-xs text-gray-400">{formatBytes(report.fileSizeBytes)}</span>}
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
