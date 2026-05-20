/**
 * 任务相关 VO/DTO — 与后端 task/vo/* 对齐
 */
import type { PublicUserVO } from './user'

export type TaskType = 'ERRAND' | 'MUTUAL_HELP' | 'TUTOR'

export type TaskStatus =
  | 'PENDING_ACCEPT'
  | 'IN_PROGRESS'
  | 'WAIT_CONFIRM'
  | 'COMPLETED'
  | 'CANCELED'
  | 'EXPIRED'

export interface TaskListItemVO {
  taskId: string
  taskType: TaskType
  title: string
  rewardPoint: number
  deadlineAt: string             // ISO 8601
  status: TaskStatus
  publisher: PublicUserVO
  building?: string
  createdAt: string
}

export interface TaskAttachment {
  url: string
  kind: 'PROOF' | 'DETAIL'
}

export interface TaskDetailVO extends TaskListItemVO {
  detail: string
  attachments: TaskAttachment[]
  acceptor?: PublicUserVO | null
  proofImages?: string[]
  proofNote?: string
  version: number                // 乐观锁
  extendCount?: number
}

export interface TaskCreateDTO {
  taskType: TaskType
  title: string
  detail: string
  rewardPoint: number
  deadlineAt: string
  building?: string
}

export interface TaskSearchParams {
  page?: number
  size?: number
  taskType?: TaskType
  status?: TaskStatus
  minCredit?: number
  deadlineFrom?: string
  deadlineTo?: string
  q?: string
}
