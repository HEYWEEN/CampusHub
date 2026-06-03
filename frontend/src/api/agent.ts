import { apiGet, apiPost } from './client'
import type { AgentChatResponse, AgentMessageVO } from '../types/agent'
import { mockAgentChat, mockAgentHistory } from './_mock'
import { withMock } from './withMock'

export const sendAgentMessage = (message: string) =>
  withMock<AgentChatResponse>(
    () => apiPost('/api/agent/chat', { message }),
    () => mockAgentChat(message),
    'agent',
  )

export const getAgentHistory = () =>
  withMock<AgentMessageVO[]>(
    () => apiGet('/api/agent/history'),
    () => mockAgentHistory(),
    'agent',
  )
