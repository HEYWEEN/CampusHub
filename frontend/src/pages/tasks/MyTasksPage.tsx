import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { searchTasks } from '../../api/task'
import { useAuthStore } from '../../stores/auth'
import { MOCK_CURRENT_USER_ID } from '../../api/_mock'
import TaskCard from '../../components/domain/TaskCard'
import './Tasks.css'

type Tab = 'published' | 'accepted'

export default function MyTasksPage() {
  const [tab, setTab] = useState<Tab>('published')
  const currentUserId = useAuthStore((s) => s.userId) ?? MOCK_CURRENT_USER_ID
  const meId = Number(currentUserId)

  // 服务端按发布者/接单者过滤（避免拉大列表客户端筛、超页漏数据）
  const { data, isLoading } = useQuery({
    queryKey: ['tasks-mine', tab, meId],
    queryFn: () => searchTasks(
      tab === 'published'
        ? { page: 1, size: 50, publisherId: meId }
        : { page: 1, size: 50, assigneeId: meId },
    ),
  })

  const filtered = data?.items ?? []

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">
          我的<span className="it">任务</span>。
        </h1>
        <div className="page-sub">发布的 · 接的</div>
      </div>

      <div className="my-tabs">
        <button
          onClick={() => setTab('published')}
          className={`my-tab${tab === 'published' ? ' is-active' : ''}`}
          type="button"
        >
          我<span className="it">发布</span>的
        </button>
        <button
          onClick={() => setTab('accepted')}
          className={`my-tab${tab === 'accepted' ? ' is-active' : ''}`}
          type="button"
        >
          我<span className="it">接</span>的
        </button>
      </div>

      {isLoading && <div className="task-loading">正在加载…</div>}

      {!isLoading && filtered.length === 0 && (
        <div className="task-empty">
          这里很<span className="it">安静。</span>{' '}
          {tab === 'published'
            ? '你还没发过任务。去 → 任务大厅 → 发布新任务'
            : '你还没接过任务。去 → 任务大厅 → 接单帮同学一把'}
        </div>
      )}

      {!isLoading && filtered.length > 0 && (
        <div className="tasks-grid">
          {filtered.map((t) => (
            <TaskCard key={t.taskId} task={t} />
          ))}
        </div>
      )}
    </div>
  )
}
