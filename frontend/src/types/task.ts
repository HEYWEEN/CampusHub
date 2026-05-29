/**
 * 任务相关 VO/DTO — 与后端 task/vo/* + dto/* 字段名严格对齐
 * （schema_audit A-5 / B-8 修复）
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

// 对齐后端 TaskListItemVO record
export interface TaskListItemVO {
  taskId: string
  title: string
  taskType: TaskType
  status: TaskStatus
  rewardPoint: number
  deadlineAt: string             // ISO 8601
  deliveryBuilding: string       // 原前端 building?，后端必返
  publisher: PublicUserVO
  createdAt: string
}

// 对齐后端 TaskDetailVO record
export interface TaskDetailVO extends TaskListItemVO {
  pickupHint: string             // 取件位置
  remark: string                 // 原前端 detail
  assignee?: PublicUserVO | null // 原前端 acceptor
  attachmentUrls: string[]       // 原前端 attachments(TaskAttachment[])
  version: number                // 乐观锁（accept/cancel 需传）
  canAccept: boolean             // 后端推导：当前用户能否接单
  isPublisher: boolean           // 后端推导：当前用户是否是发布者
}

// 对齐后端 TaskCreateDTO
export interface TaskCreateDTO {
  taskType: TaskType
  title: string
  rewardPoint: number
  deadlineAt: string
  pickupHint: string             // 必填
  deliveryBuilding: string       // 必填
  remark?: string                // 可选
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
