import { apiGet, apiPost, apiPatch } from './client'
import { BizError, type PageResponse } from '../types/api'
import type {
  TaskCreateDTO,
  TaskDetailVO,
  TaskListItemVO,
  TaskSearchParams,
} from '../types/task'
import {
  mockAcceptTask,
  mockCancelTask,
  mockConfirmTask,
  mockCreateTask,
  mockGetTask,
  mockSearchTasks,
  mockSubmitProof,
} from './_mock'

/**
 * dev 模式下，API 调用失败时自动回退到 mock；
 * 生产环境直接 throw，保证联调时能暴露真实错误。
 */
async function withMock<T>(real: () => Promise<T>, mock: () => T): Promise<T> {
  if (!import.meta.env.DEV) return real()
  try {
    return await real()
  } catch (err) {
    if (err instanceof BizError && err.httpStatus !== undefined && err.httpStatus < 500) {
      // 业务错码（400/403/409 等）保留真实错误，不用 mock 掩盖
      throw err
    }
    // 网络错或 5xx：fallback 到 mock
    // eslint-disable-next-line no-console
    console.warn('[mock] 后端未响应，使用 mock 数据')
    return mock()
  }
}

export const searchTasks = (params: TaskSearchParams) =>
  withMock<PageResponse<TaskListItemVO>>(
    () => apiGet('/api/search/tasks', params),
    () => mockSearchTasks(params),
  )

export const getTask = (taskId: string) =>
  withMock<TaskDetailVO>(
    () => apiGet(`/api/tasks/${taskId}`),
    () => mockGetTask(taskId),
  )

export const createTask = (dto: TaskCreateDTO) =>
  withMock<{ taskId: string }>(
    () => apiPost('/api/tasks', dto),
    () => ({ taskId: mockCreateTask(dto) }),
  )

export const acceptTask = (taskId: string, version: number) =>
  withMock<void>(
    () => apiPost(`/api/tasks/${taskId}/accept`, { version }),
    () => { mockAcceptTask(taskId) },
  )

// schema_audit A-8 修复：后端字段名 text（不是 note），且 multipart 已改为 JSON
export const submitProof = (taskId: string, images: string[], text: string) =>
  withMock<void>(
    () => apiPost(`/api/tasks/${taskId}/proof`, { images, text }),
    () => { mockSubmitProof(taskId, images, text) },
  )

export const confirmTask = (taskId: string) =>
  withMock<void>(
    () => apiPost(`/api/tasks/${taskId}/confirm`),
    () => { mockConfirmTask(taskId) },
  )

export const cancelTask = (taskId: string, reason?: string) =>
  withMock<void>(
    () => apiPost(`/api/tasks/${taskId}/cancel`, { reason: reason ?? '' }),
    () => { mockCancelTask(taskId) },
  )

export const updateTask = (taskId: string, patch: Partial<TaskCreateDTO>) =>
  apiPatch<void>(`/api/tasks/${taskId}`, patch)
