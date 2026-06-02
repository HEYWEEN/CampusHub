import { apiGet, apiPatch } from './client'
import type { AdminUserVO, AdminVerificationVO } from '../types/admin'
import type { CreditAppealVO } from '../types/credit'
import {
  mockAdminAppeals,
  mockAdminSearchUsers,
  mockAdminVerifications,
} from './_mock'
import { withMock } from './withMock'

// 认证审核
export const adminListVerifications = () =>
  withMock<AdminVerificationVO[]>(
    () => apiGet('/api/admin/verifications'),
    () => mockAdminVerifications(),
    'admin',
  )

export const adminApproveVerification = (id: number) =>
  withMock<void>(
    () => apiPatch(`/api/admin/verifications/${id}/approve`),
    () => undefined,
    'admin',
  )

export const adminRejectVerification = (id: number, reason: string) =>
  withMock<void>(
    () => apiPatch(`/api/admin/verifications/${id}/reject`, { reason }),
    () => undefined,
    'admin',
  )

// 用户管理
export const adminSearchUsers = (q: string) =>
  withMock<AdminUserVO[]>(
    () => apiGet('/api/admin/users', { q }),
    () => mockAdminSearchUsers(q),
    'admin',
  )

export const adminSetBan = (userId: number, banned: boolean, reason?: string) =>
  withMock<AdminUserVO>(
    () => apiPatch(`/api/admin/users/${userId}/ban`, { banned, reason }),
    () => ({ userId, nickname: 'mock', avatarUrl: null, verifyStatus: 'approved', banned, role: 'USER' }),
    'admin',
  )

// 申诉裁决
export const adminListAppeals = () =>
  withMock<CreditAppealVO[]>(
    () => apiGet('/api/admin/appeals'),
    () => mockAdminAppeals(),
    'admin',
  )

export const adminResolveAppeal = (id: number, approve: boolean, note?: string) =>
  withMock<void>(
    () => apiPatch(`/api/admin/appeals/${id}`, { approve, note }),
    () => undefined,
    'admin',
  )
