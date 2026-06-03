import { apiGet } from './client'
import type { TaskListItemVO } from '../types/task'
import { mockRecommendedTasks } from './_mock'
import { withMock } from './withMock'

/**
 * 智能匹配 / 为你推荐（P2）。后端按规则加权打分返回排序后的待接单任务。
 * 需登录（后端在 /api/** 下鉴权）。
 */
export const getRecommendedTasks = (limit = 8) =>
  withMock<TaskListItemVO[]>(
    () => apiGet('/api/recommend/tasks', { limit }),
    () => mockRecommendedTasks(limit),
    'recommend',
  )
