import { apiClient } from './client';
import type { GeneratedReport } from '../types';
import type { EventFilters } from '../types';

export interface GenerateReportRequest {
  name:       string;
  templateId?: string;
  format:     'PDF' | 'CSV' | 'XLSX';
  filters:    Partial<EventFilters> & { startTime: string; endTime: string };
}

export const reportsApi = {
  generate: (data: GenerateReportRequest) =>
    apiClient.post<ApiResponse<GeneratedReport>>('/reports', data).then((r) => r.data.data),

  list: (params?: { page?: number; size?: number; status?: string }) =>
    apiClient.get<ApiResponse<{ content: GeneratedReport[]; totalElements: number; totalPages: number }>>(
      '/reports', { params }
    ).then((r) => r.data.data),

  getById: (reportId: string) =>
    apiClient.get<ApiResponse<GeneratedReport>>(`/reports/${reportId}`).then((r) => r.data.data),

  listTemplates: () =>
    apiClient.get<ApiResponse<{ id: string; name: string; templateType: string; format: string }[]>>(
      '/reports/templates'
    ).then((r) => r.data.data),
};

interface ApiResponse<T> {
  success: boolean;
  data: T;
}
