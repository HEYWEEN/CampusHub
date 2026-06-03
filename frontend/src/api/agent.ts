import { apiGet, apiPost } from './client'
import type { AgentChatResponse, AgentHistory } from '../types/agent'
import { mockAgentChat, mockAgentHistory } from './_mock'
import { withMock } from './withMock'

// conversationId 为空 = 开启新对话
export const sendAgentMessage = (message: string, conversationId?: number | null) =>
  withMock<AgentChatResponse>(
    () => apiPost('/api/agent/chat', { message, conversationId: conversationId ?? null }),
    () => mockAgentChat(message),
    'agent',
  )

export const getAgentHistory = () =>
  withMock<AgentHistory>(
    () => apiGet('/api/agent/history'),
    () => mockAgentHistory(),
    'agent',
  )
