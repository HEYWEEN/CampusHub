import { apiPost } from './client'
import type { TokenPair } from '../types/api'

/**
 * 鉴权 API — 对应 openapi.yaml /api/auth/*
 */

export const sendSmsCode = (phone: string) =>
  apiPost<void>('/api/auth/sms-codes', { phone })

export const loginByCode = (phone: string, code: string) =>
  apiPost<TokenPair>('/api/auth/token', { phone, code })

export const loginByPassword = (phone: string, password: string) =>
  apiPost<TokenPair>('/api/auth/token/password', { phone, password })

export const register = (phone: string, code: string, password: string) =>
  apiPost<TokenPair>('/api/auth/register', { phone, smsCode: code, password })

export interface VerifySubmitDTO {
  realName: string
  studentNo: string
  certKind: 'STUDENT_CARD' | 'ID_BACK'
  images: string[]   // 已上传的图片 URL（≤ 3）
}

export const submitVerification = (dto: VerifySubmitDTO) =>
  apiPost<{ ticketId: string }>('/api/auth/verifications', dto)

export const getMyVerification = () =>
  apiPost<{ status: 'pending' | 'approved' | 'rejected'; rejectReason?: string }>(
    '/api/auth/verifications/me',
  )

export const logout = () => apiPost<void>('/api/auth/logout')
