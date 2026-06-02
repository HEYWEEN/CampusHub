/** 管理端 VO（对齐后端 admin / auth.AdminVerificationVO）。 */
import type { VerifyStatus } from './user'

export interface AdminVerificationVO {
  verificationId: number
  userId: number
  realName: string | null
  studentNo: string | null
  idCard: string | null
  attachmentUrls: string[]
  status: 'pending' | 'approved' | 'rejected'
  createdAt: string
}

export interface AdminUserVO {
  userId: number
  nickname: string | null
  avatarUrl: string | null
  verifyStatus: VerifyStatus
  banned: boolean
  role: 'USER' | 'ADMIN'
}
