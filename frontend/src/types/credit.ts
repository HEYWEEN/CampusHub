/**
 * 信用 / 积分相关 VO/DTO
 */

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
