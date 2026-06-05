import type { TaskListItemVO, TaskType } from './task'
import type { TradeItemVO } from './trade'
import type { TeamRecruitVO } from './team'

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
  type: 'task_results' | 'task_draft' | 'trade_results' | 'team_results'
  tasks?: TaskListItemVO[]
  draft?: TaskDraftVO
  items?: TradeItemVO[]
  teams?: TeamRecruitVO[]
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
