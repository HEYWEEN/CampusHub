import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { searchTasks } from '../../api/task'
import type { TaskStatus } from '../../types/task'
import TaskCard from '../../components/domain/TaskCard'
import '../tasks/Tasks.css'

const STATUSES: { value: TaskStatus | null; label: string }[] = [
  { value: 'PENDING_ACCEPT', label: '可接' },
  { value: null, label: '全部' },
  { value: 'COMPLETED', label: '已完成' },
]

export default function EduTutorHallPage() {
  const [status, setStatus] = useState<TaskStatus | null>('PENDING_ACCEPT')
  const [q, setQ] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['tasks', { type: 'TUTOR', status, q }],
    queryFn: () => searchTasks({
      taskType: 'TUTOR',
      status: status ?? undefined,
      q: q || undefined,
      page: 1, size: 12,
    }),
  })

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">
          找位<span className="it">学长 / 学姐</span>。
        </h1>
        <div className="page-sub">辅导大厅 · 课程对口 · 信用透明</div>
      </div>

      <div className="task-toolbar">
        <div className="filter-group">
          {STATUSES.map((s) => (
            <button
              key={String(s.value)}
              type="button"
              onClick={() => setStatus(s.value)}
              className={`filter-pill${status === s.value ? ' is-active' : ''}`}
            >
              {s.label}
            </button>
          ))}
        </div>
        <input
          type="search"
          className="task-search"
          placeholder="搜索课程：高数 / 操作系统 / 机器学习…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <div className="toolbar-spacer" />
        <Link to="/app/edu/tutor/new" className="toolbar-new-btn">
          发布辅导需求 <span className="arr">→</span>
        </Link>
      </div>

      {isLoading && <div className="task-loading">正在加载…</div>}

      {data && data.items.length === 0 && (
        <div className="task-empty">
          这里很<span className="it">安静。</span> 你可以来当第一个学长。
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
