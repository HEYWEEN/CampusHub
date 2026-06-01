import { apiGet, apiPatch, apiPost } from './client'
import type { NotifyMessageVO } from '../types/credit'
import {
  mockListNotifications,
  mockMarkAllRead,
  mockMarkRead,
  mockUnreadCount,
} from './_mock'
import { withMock } from './withMock'

export const listNotifications = (filter?: 'all' | 'unread' | 'read') =>
  withMock<NotifyMessageVO[]>(
    () => apiGet('/api/notify/messages', { filter }),
    () => mockListNotifications(filter),
  )

export const getUnreadCount = () =>
  withMock<{ count: number }>(
    () => apiGet('/api/notify/messages/unread-count'),
    () => ({ count: mockUnreadCount() }),
  )

export const markAsRead = (id: string) =>
  withMock<void>(
    () => apiPatch(`/api/notify/messages/${id}/read`),
    () => { mockMarkRead(id) },
  )

export const markAllAsRead = () =>
  withMock<void>(
    () => apiPost('/api/notify/messages/read-all'),
    () => { mockMarkAllRead() },
  )
