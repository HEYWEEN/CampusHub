import type { PublicUserVO } from './user'

export type ReportTargetType = 'TASK' | 'TRADE' | 'USER'
export type ReportStatus = 'PENDING' | 'RESOLVED' | 'DISMISSED'
export type ReportDecisionType = 'DISMISS' | 'WARN' | 'PENALIZE'

// 对齐后端 ReportCaseVO
export interface ReportCaseVO {
  caseId: number
  reporter: PublicUserVO | null   // admin 队列视图填充；我的举报为 null
  targetType: ReportTargetType
  targetId: number
  reasonCategory: string
  description: string | null
  evidenceUrls: string[]
  status: ReportStatus
  decisionType: ReportDecisionType | null
  createdAt: string
}

// 对齐后端 ReportCreateDTO
export interface ReportCreateDTO {
  targetType: ReportTargetType
  targetId: number
  reasonCategory: string
  description?: string
  evidenceUrls?: string[]
}
