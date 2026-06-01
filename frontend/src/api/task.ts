import { apiGet, apiPost, apiPatch } from './client'
import { type PageResponse } from '../types/api'
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
import { withMock } from './withMock'

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
