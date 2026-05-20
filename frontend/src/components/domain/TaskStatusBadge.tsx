import type { TaskStatus } from '../../types/task'
import './Domain.css'

const LABEL: Record<TaskStatus, { text: string; tone: string }> = {
  PENDING_ACCEPT: { text: '待接单', tone: 'pending' },
  IN_PROGRESS:    { text: '进行中', tone: 'progress' },
  WAIT_CONFIRM:   { text: '待确认', tone: 'wait' },
  COMPLETED:      { text: '已完成', tone: 'done' },
  CANCELED:       { text: '已取消', tone: 'canceled' },
  EXPIRED:        { text: '已超时', tone: 'expired' },
}

export default function TaskStatusBadge({ status }: { status: TaskStatus }) {
  const { text, tone } = LABEL[status]
  return <span className={`status-badge status-${tone}`}>{text}</span>
}
