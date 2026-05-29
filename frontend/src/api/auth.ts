import { apiGet, apiPost } from './client'
import type { TokenPair } from '../types/api'

/**
 * 鉴权 API — 对应 openapi.yaml /api/auth/*
 */

export const sendSmsCode = (phone: string) =>
  apiPost<void>('/api/auth/sms-codes', { phone })

export const loginByCode = (phone: string, code: string) =>
  apiPost<TokenPair>('/api/auth/token', { phone, smsCode: code })

export const loginByPassword = (phone: string, password: string) =>
  apiPost<TokenPair>('/api/auth/token/password', { phone, password })

export const register = (phone: string, code: string, password: string) =>
  apiPost<TokenPair>('/api/auth/register', { phone, smsCode: code, password })

/**
 * 对齐后端 VerificationSubmitDTO（schema_audit A-12 修复后）。
 * 学生证图片走 ImageUploader 上传到 /api/uploads 拿 URL，再放在 attachmentUrls 里提交。
 */
export interface VerifySubmitDTO {
  realName: string
  studentNo: string
  idCard?: string
  attachmentUrls: string[]   // 1~5 张证件图 URL
}

export const submitVerification = (dto: VerifySubmitDTO) =>
  apiPost<VerificationStatusVO>('/api/auth/verifications', dto)

/**
 * 对齐后端 VerificationStatusVO（schema_audit A-11 / B-7 修复）：
 *   - 方法从 POST 改为 GET
 *   - 字段 status 从 'pending'|... 等小写（后端 @JsonValue 序列化为小写）
 */
export interface VerificationStatusVO {
  id?: number | null
  status: 'pending' | 'approved' | 'rejected' | null
  rejectReason?: string | null
  attachmentUrls?: string[] | null
  createdAt?: string | null
  updatedAt?: string | null
}

export const getMyVerification = () =>
  apiGet<VerificationStatusVO>('/api/auth/verifications/me')

/**
 * Dev-only：把自己的最新 PENDING 认证一键通过（绕过管理员）。
 * 后端走 @Profile("!prod")，生产返回 404。
 */
export const devApproveVerification = () =>
  apiPost<VerificationStatusVO>('/api/auth/verifications/me/dev-approve')

/**
 * 退出登录 — 传 refreshToken 让后端把 refresh jti 入黑名单
 * （schema_audit A-13 修复：原本不传，refresh token 14 天内可被重放）。
 */
export const logout = (refreshToken?: string | null) =>
  apiPost<void>('/api/auth/logout', refreshToken ? { refreshToken } : undefined)
