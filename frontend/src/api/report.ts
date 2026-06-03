import { apiGet, apiPost } from './client'
import type { ReportCaseVO, ReportCreateDTO, ReportDecisionType } from '../types/report'
import { mockAdminReports, mockMyReports, mockSubmitReport } from './_mock'
import { withMock } from './withMock'

// 用户侧
export const submitReport = (dto: ReportCreateDTO) =>
  withMock<ReportCaseVO>(
    () => apiPost('/api/reports', dto),
    () => mockSubmitReport(dto),
    'report',
  )

export const listMyReports = () =>
  withMock<ReportCaseVO[]>(
    () => apiGet('/api/reports/me'),
    () => mockMyReports(),
    'report',
  )

// 管理端
export const adminListReports = () =>
  withMock<ReportCaseVO[]>(
    () => apiGet('/api/admin/reports'),
    () => mockAdminReports(),
    'report',
  )

export const adminDecideReport = (
  caseId: number,
  decisionType: ReportDecisionType,
  penaltyPoints?: number,
  reason?: string,
) =>
  withMock<void>(
    () => apiPost(`/api/admin/reports/${caseId}/decision`, { decisionType, penaltyPoints, reason }),
    () => undefined,
    'report',
  )
