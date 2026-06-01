import { Link } from 'react-router-dom'
import type { TaskListItemVO } from '../../types/task'
import PublicUserCard from './PublicUserCard'
import TaskStatusBadge from './TaskStatusBadge'
import { formatDeadline } from '../../utils/format'
import { TASK_TYPE_LABEL } from '../../utils/labels'
import './Domain.css'

export default function TaskCard({ task }: { task: TaskListItemVO }) {
  const dl = formatDeadline(task.deadlineAt)
  return (
    <Link to={`/app/tasks/${task.taskId}`} className="task-card">
      <div className="task-card-head">
        <span className={`task-type task-type-${task.taskType.toLowerCase()}`}>
          {TASK_TYPE_LABEL[task.taskType]}
        </span>
        <TaskStatusBadge status={task.status} />
      </div>

      <h3 className="task-card-title">{task.title}</h3>

      <div className="task-card-meta">
        {task.deliveryBuilding && <span className="task-meta-item">📍 {task.deliveryBuilding}</span>}
        <span className={`task-meta-item${dl.urgent ? ' task-meta-urgent' : ''}${dl.expired ? ' task-meta-expired' : ''}`}>
          ⏰ 截止 {dl.text}
        </span>
      </div>

      <div className="task-card-foot">
        <PublicUserCard user={task.publisher} size="sm" />
        <span className="task-reward">
          <span className="task-reward-num">{task.rewardPoint}</span>
          <span className="task-reward-unit">积分</span>
        </span>
      </div>
    </Link>
  )
}
