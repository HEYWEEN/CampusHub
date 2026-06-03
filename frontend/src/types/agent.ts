import type { TaskListItemVO, TaskType } from './task'

/** 发单草稿（对齐后端 agent.vo.TaskDraftVO）。 */
export interface TaskDraftVO {
  title: string
  taskType: TaskType
  rewardPoint: number
  deadlineIso: string
  deliveryBuilding?: string | null
  remark?: string | null
}

/** 助手回复里的结构化动作。 */
export interface AgentAction {
  type: 'task_results' | 'task_draft'
  tasks?: TaskListItemVO[]
  draft?: TaskDraftVO
}

export interface AgentChatResponse {
  conversationId: number
  reply: string
  actions: AgentAction[]
}

export interface AgentMessageVO {
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

export interface AgentHistory {
  conversationId: number | null
  messages: AgentMessageVO[]
}
