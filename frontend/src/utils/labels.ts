import type { TaskType } from '../types/task'
import type { TradeItemStatus } from '../types/trade'

/** 任务类型 → 中文标签（任务卡片与详情页共用）。 */
export const TASK_TYPE_LABEL: Record<TaskType, string> = {
  ERRAND: '跑腿',
  MUTUAL_HELP: '互助',
  TUTOR: '辅导',
}

/** 交易状态 → 标签文案 + 徽章色调（商品卡片与详情页共用）。 */
export const TRADE_STATUS_LABEL: Record<TradeItemStatus, { text: string; tone: string }> = {
  ON_SALE: { text: '在售', tone: 'pending' },
  IN_TRADE: { text: '交易中', tone: 'progress' },
  OFF_SALE: { text: '已下架', tone: 'canceled' },
}
