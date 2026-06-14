import { useState } from 'react';
import { Plus } from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '../components/ui/Button';
import { ReportList } from '../components/reports/ReportList';
import { GenerateReportModal } from '../components/reports/GenerateReportModal';
import { reportsApi } from '../api/reports.api';
import { TableSkeleton } from '../components/ui/Skeleton';

export function ReportsPage() {
  const [showModal, setShowModal] = useState(false);
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['reports'],
    queryFn:  () => reportsApi.list({ size: 20 }),
    refetchInterval: 10_000, // auto-refresh for in-progress reports
  });

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Compliance Reports</h1>
          <p className="text-sm text-gray-500 mt-0.5">Generate PDF, CSV, or Excel reports for audits</p>
        </div>
        <Button icon={<Plus className="h-4 w-4" />} onClick={() => setShowModal(true)}>
          New Report
        </Button>
      </div>

      <div className="rounded-xl border border-gray-100 bg-white shadow-card px-2">
        {isLoading ? <TableSkeleton rows={5} cols={4} /> : (
          <ReportList
            reports={data?.content ?? []}
            onRefresh={() => qc.invalidateQueries({ queryKey: ['reports'] })}
          />
        )}
      </div>

      <GenerateReportModal open={showModal} onClose={() => setShowModal(false)} />
    </div>
  );
}
