/**
 * 信用 / 积分相关 VO/DTO
 */
import type { PublicUserVO } from './user'

export type CreditAppealStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

// 对齐后端 CreditAppealVO
export interface CreditAppealVO {
  appealId: number
  reviewId: number
  reviewRating: number | null
  reviewComment: string | null
  reason: string
  evidenceUrls: string[]
  status: CreditAppealStatus
  resolveNote: string | null
  appellant: PublicUserVO | null   // 仅 admin 视图
  createdAt: string
}

// 对齐后端 ReviewResultVO（提交评价响应）
export interface ReviewResultVO {
  reviewId: number
  bothReviewed: boolean   // 双方都已互评 → 各 +1 信用分
}

// 对齐后端 ReceivedReviewVO（申诉入口）
export interface ReceivedReviewVO {
  reviewId: number
  taskId: number
  reviewer: PublicUserVO
  rating: number
  comment: string
  voided: boolean
  underAppeal: boolean
  appealable: boolean
  createdAt: string
}

export interface CreditMeVO {
  userId: string
  creditScore: number            // 信用分
  pointBalance: number           // 可用积分
  pointFrozen: number            // 冻结积分
  dailyAcceptLimit: number
  canPublish: boolean            // creditScore >= 60
  canAccept: boolean             // creditScore >= 60
}

export type CreditRecordDirection = 'FREEZE' | 'UNFREEZE' | 'SETTLE' | 'DEDUCT'

export interface CreditRecord {
  id: string
  direction: CreditRecordDirection
  delta: number
  reasonCode: string
  bizId?: string
  createdAt: string
}

export interface AppealVO {
  appealId: string
  badReviewId: string
  status: 'pending' | 'upheld' | 'rejected'
  evidence: string[]             // 图片 URL
  statement: string
  submittedAt: string
  decidedAt?: string
  decisionReason?: string
}

export interface NotifyMessageVO {
  id: string
  type: string
  title: string
  body: string
  readAt?: string | null
  createdAt: string
  bizId?: string
}
