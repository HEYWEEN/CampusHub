import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { searchTasks } from '../../api/task'
import type { TaskStatus, TaskType } from '../../types/task'
import TaskCard from '../../components/domain/TaskCard'
import './Tasks.css'

const TYPES: { value: TaskType | null; label: string }[] = [
  { value: null, label: '全部' },
  { value: 'ERRAND', label: '跑腿' },
  { value: 'MUTUAL_HELP', label: '互助' },
  { value: 'TUTOR', label: '辅导' },
]

const STATUSES: { value: TaskStatus | null; label: string }[] = [
  { value: 'PENDING_ACCEPT', label: '可接' },
  { value: null, label: '全部' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'COMPLETED', label: '已完成' },
]

export default function TaskHallPage() {
  const [type, setType] = useState<TaskType | null>(null)
  const [status, setStatus] = useState<TaskStatus | null>('PENDING_ACCEPT')
  const [q, setQ] = useState('')

  const { data, isLoading, error } = useQuery({
    queryKey: ['tasks', { type, status, q }],
    queryFn: () => searchTasks({
      taskType: type ?? undefined,
      status: status ?? undefined,
      q: q || undefined,
      page: 1,
      size: 12,
    }),
  })

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">
          找点<span className="it">事</span>做。
        </h1>
        <div className="page-sub">任务大厅 · 跑腿 · 互助 · 辅导</div>
      </div>

      <div className="task-toolbar">
        <div className="filter-group">
          {TYPES.map((t) => (
            <button
              key={String(t.value)}
              onClick={() => setType(t.value)}
              className={`filter-pill${type === t.value ? ' is-active' : ''}`}
              type="button"
            >
              {t.label}
            </button>
          ))}
        </div>
        <div className="filter-group">
          {STATUSES.map((s) => (
            <button
              key={String(s.value)}
              onClick={() => setStatus(s.value)}
              className={`filter-pill${status === s.value ? ' is-active' : ''}`}
              type="button"
            >
              {s.label}
            </button>
          ))}
        </div>
        <input
          type="search"
          className="task-search"
          placeholder="搜索标题或备注…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <div className="toolbar-spacer" />
        <Link to="/app/tasks/new" className="toolbar-new-btn">
          发布新任务 <span className="arr">→</span>
        </Link>
      </div>

      {isLoading && <div className="task-loading">正在加载…</div>}

      {error && (
        <div className="task-error">
          加载失败：{(error as Error).message}
        </div>
      )}

      {data && data.items.length === 0 && (
        <div className="task-empty">
          这里很<span className="it">安静。</span> 当前筛选下没有任务。
        </div>
      )}

      {data && data.items.length > 0 && (
        <div className="tasks-grid">
          {data.items.map((t) => (
            <TaskCard key={t.taskId} task={t} />
          ))}
        </div>
      )}
    </div>
  )
}
