import { Link } from 'react-router-dom'
import type { TaskListItemVO, TaskType } from '../../types/task'
import PublicUserCard from './PublicUserCard'
import TaskStatusBadge from './TaskStatusBadge'
import { formatDeadline } from '../../utils/format'
import './Domain.css'

const TYPE_LABEL: Record<TaskType, string> = {
  ERRAND: '跑腿',
  MUTUAL_HELP: '互助',
  TUTOR: '辅导',
}

export default function TaskCard({ task }: { task: TaskListItemVO }) {
  const dl = formatDeadline(task.deadlineAt)
  return (
    <Link to={`/app/tasks/${task.taskId}`} className="task-card">
      <div className="task-card-head">
        <span className={`task-type task-type-${task.taskType.toLowerCase()}`}>
          {TYPE_LABEL[task.taskType]}
        </span>
        <TaskStatusBadge status={task.status} />
      </div>

      <h3 className="task-card-title">{task.title}</h3>

      <div className="task-card-meta">
        {task.building && <span className="task-meta-item">📍 {task.building}</span>}
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
