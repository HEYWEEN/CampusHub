import { apiGet, apiPost } from './client'
import { type PageResponse } from '../types/api'
import type { ImConversationVO, ImMessageType, ImMessageVO } from '../types/im'
import {
  mockGetMessages,
  mockGetUnread,
  mockListConversations,
  mockSendMessage,
  mockStartConversation,
} from './_mock'
import { withMock } from './withMock'

export const listConversations = () =>
  withMock<ImConversationVO[]>(
    () => apiGet('/api/im/conversations'),
    () => mockListConversations(),
    'im',
  )

export const startConversation = (peerId: number | string) =>
  withMock<ImConversationVO>(
    () => apiPost('/api/im/conversations', { peerId: Number(peerId) }),
    () => mockStartConversation(Number(peerId)),
    'im',
  )

export const getMessages = (conversationId: number | string, page = 1, size = 30) =>
  withMock<PageResponse<ImMessageVO>>(
    () => apiGet(`/api/im/conversations/${conversationId}/messages?page=${page}&size=${size}`),
    () => mockGetMessages(Number(conversationId)),
    'im',
  )

export const sendMessage = (
  conversationId: number | string,
  content: string,
  contentType: ImMessageType = 'TEXT',
) =>
  withMock<ImMessageVO>(
    () => apiPost(`/api/im/conversations/${conversationId}/messages`, { content, contentType }),
    () => mockSendMessage(content, contentType),
    'im',
  )

export const getUnread = () =>
  withMock<{ count: number }>(
    () => apiGet('/api/im/unread'),
    () => mockGetUnread(),
    'im',
  )
