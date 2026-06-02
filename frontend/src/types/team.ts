/**
 * 组队相关 VO / DTO — 与后端 team/vo + dto 字段名严格对齐。
 */
import type { PublicUserVO } from './user'

export type TeamRecruitStatus = 'RECRUITING' | 'FULL' | 'CLOSED'
export type TeamApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

// 对齐后端 TeamRecruitVO record
export interface TeamRecruitVO {
  recruitId: number
  title: string
  description: string | null
  skillTags: string[]
  totalSize: number
  currentSize: number
  status: TeamRecruitStatus
  creator: PublicUserVO
  createdAt: string
  isCreator: boolean
  myApplicationStatus: TeamApplicationStatus | null
}

// 对齐后端 TeamApplicationVO record
export interface TeamApplicationVO {
  applicationId: number
  applicant: PublicUserVO
  creditScore: number
  message: string | null
  status: TeamApplicationStatus
  createdAt: string
}

// 对齐后端 TeamRecruitCreateDTO
export interface TeamRecruitCreateDTO {
  title: string
  description?: string
  skillTags: string[]
  totalSize: number
}

export interface TeamSearchParams {
  page?: number
  size?: number
  q?: string
  tag?: string
  status?: TeamRecruitStatus
}
