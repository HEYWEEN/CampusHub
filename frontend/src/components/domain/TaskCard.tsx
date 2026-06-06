import { Link } from 'react-router-dom'
import type { TaskListItemVO } from '../../types/task'
import PublicUserCard from './PublicUserCard'
import TaskStatusBadge from './TaskStatusBadge'
import { formatDeadline, formatRelativeTime } from '../../utils/format'
import { TASK_TYPE_LABEL } from '../../utils/labels'
import './Domain.css'

export default function TaskCard({
  task,
  context,
}: {
  task: TaskListItemVO
  /** published：我发布的（展示接单者）；accepted/缺省：展示发布者 */
  context?: 'published' | 'accepted'
}) {
  const dl = formatDeadline(task.deadlineAt)
  const openable = task.status === 'PENDING_ACCEPT'
  const showAssignee = context === 'published'
  // 辅导任务走辅导专属 URL，顶栏高亮“辅导”
  const detailPath = task.taskType === 'TUTOR'
    ? `/app/edu/tutor/${task.taskId}`
    : `/app/tasks/${task.taskId}`
  return (
    <Link to={detailPath} className="task-card">
      <div className="task-card-head">
        <span className={`task-type task-type-${task.taskType.toLowerCase()}`}>
          {TASK_TYPE_LABEL[task.taskType]}
        </span>
        <TaskStatusBadge status={task.status} />
      </div>

      <h3 className="task-card-title">{task.title}</h3>
      {task.remark && <p className="task-card-desc">{task.remark}</p>}

      <div className="task-card-meta">
        {task.deliveryBuilding && <span className="task-meta-item">📍 {task.deliveryBuilding}</span>}
        <span className={`task-meta-item${dl.urgent ? ' task-meta-urgent' : ''}${dl.expired ? ' task-meta-expired' : ''}`}>
          ⏰ 截止 {dl.text}
        </span>
        <span className="task-meta-item task-meta-time">{formatRelativeTime(task.createdAt)}发布</span>
      </div>

      <div className="task-card-foot">
        {showAssignee ? (
          task.assignee ? (
            <span className="task-card-party">
              <span className="task-card-party-label">接单者</span>
              <PublicUserCard user={task.assignee} size="sm" />
            </span>
          ) : (
            <span className="task-card-party task-card-party-empty">待接单</span>
          )
        ) : (
          <PublicUserCard user={task.publisher} size="sm" />
        )}
        <div className="task-card-foot-right">
          <span className="task-reward">
            <span className="task-reward-num">{task.rewardPoint}</span>
            <span className="task-reward-unit">积分</span>
          </span>
          <span className={`task-card-action${openable ? ' is-primary' : ''}`}>
            {openable ? '立即接单' : '查看详情'}
          </span>
        </div>
      </div>
    </Link>
  )
}
