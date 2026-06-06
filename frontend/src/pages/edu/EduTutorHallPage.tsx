import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { searchTasks } from '../../api/task'
import type { TaskStatus } from '../../types/task'
import TaskCard from '../../components/domain/TaskCard'
import Select from '../../components/Select'
import { StatIcon, SafetyCard } from '../../components/HallWidgets'
import { formatRelativeTime } from '../../utils/format'
import '../tasks/Tasks.css'

const STATUS_OPTS = [
  { value: 'PENDING_ACCEPT', label: '可接单' },
  { value: 'ALL', label: '全部状态' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'COMPLETED', label: '已完成' },
] as const

const SORT_OPTS = [
  { value: 'latest', label: '最新发布' },
  { value: 'reward', label: '积分最高' },
] as const

type SortKey = 'latest' | 'reward'

const tutor = (extra: Record<string, unknown>) => searchTasks({ taskType: 'TUTOR', page: 1, ...extra })

export default function EduTutorHallPage() {
  const [status, setStatus] = useState<TaskStatus | null>('PENDING_ACCEPT')
  const [q, setQ] = useState('')
  const [sort, setSort] = useState<SortKey>('latest')

  const { data, isLoading } = useQuery({
    queryKey: ['tasks', 'tutor', { status, q }],
    queryFn: () => tutor({ status: status ?? undefined, q: q || undefined, size: 12 }),
  })

  const items = useMemo(() => {
    if (!data) return []
    const arr = [...data.items]
    arr.sort((a, b) => sort === 'reward'
      ? b.rewardPoint - a.rewardPoint
      : new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    return arr
  }, [data, sort])

  const { data: stats } = useQuery({
    queryKey: ['tasks', 'tutor', 'stats'],
    staleTime: 60_000,
    queryFn: async () => {
      const [pending, inProgress, completed] = await Promise.all([
        tutor({ status: 'PENDING_ACCEPT', size: 1 }),
        tutor({ status: 'IN_PROGRESS', size: 1 }),
        tutor({ status: 'COMPLETED', size: 1 }),
      ])
      return { pending: pending.total, inProgress: inProgress.total, completed: completed.total }
    },
  })

  const { data: recent } = useQuery({
    queryKey: ['tasks', 'tutor', 'recent'],
    staleTime: 30_000,
    queryFn: () => tutor({ size: 8 }),
  })
  const recentItems = recent?.items ?? []

  const STAT_CARDS = [
    { num: stats?.pending, label: '可约辅导', icon: 'book' as const },
    { num: stats?.inProgress, label: '进行中', icon: 'clock' as const },
    { num: stats?.completed, label: '已完成', icon: 'check' as const },
  ]

  return (
    <div className="wrap hall-grid">
      <div className="hall-main">
        <section className="hall-hero">
          <div className="hall-hero-lead">
            <h1 className="page-title">
              学业<span className="it">辅导</span>
            </h1>
            <p className="hall-hero-sub">课程对口、信用透明，约一次靠谱的学业辅导。</p>
            <div className="hall-cta">
              <Link to="/app/edu/tutor/new" className="hall-cta-primary">发布辅导 <span aria-hidden>+</span></Link>
            </div>
          </div>

          <div className="hall-stats">
            {STAT_CARDS.map((s) => (
              <div className="stat" key={s.label}>
                <span className="stat-ic" aria-hidden><StatIcon name={s.icon} /></span>
                <div className="stat-num">{s.num ?? '—'}</div>
                <div className="stat-label">{s.label}</div>
              </div>
            ))}
          </div>

          <div className="hall-illo-wrap" aria-hidden>
            <img className="hall-illo" src="/illustrations/reading.png" alt="" />
          </div>
        </section>

        <div className="task-toolbar" id="hall-list">
          <Select
            value={status ?? 'ALL'}
            options={STATUS_OPTS}
            onChange={(v) => setStatus(v === 'ALL' ? null : v as TaskStatus)}
            ariaLabel="状态筛选"
          />
          <input
            type="search"
            className="task-search"
            placeholder="搜索课程：高数 / 操作系统 / 机器学习…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
          <div className="toolbar-spacer" />
          <Select value={sort} options={SORT_OPTS} onChange={(v) => setSort(v as SortKey)} ariaLabel="排序" />
        </div>

        {isLoading && <div className="task-loading">正在加载…</div>}

        {data && items.length === 0 && (
          <div className="task-empty">
            这里很<span className="it">安静。</span> 你可以来当第一个学长。
          </div>
        )}

        {items.length > 0 && (
          <div className="tasks-grid">
            {items.map((t) => (
              <TaskCard key={t.taskId} task={t} />
            ))}
          </div>
        )}
      </div>

      <aside className="hall-aside">
        <section className="aside-card">
          <div className="aside-head">
            <h2 className="aside-title">辅导动态</h2>
          </div>
          {recentItems.length === 0 ? (
            <div className="aside-empty">暂无动态</div>
          ) : (
            <ul className="feed-list">
              {recentItems.slice(0, 6).map((t) => (
                <li key={t.taskId} className="feed-item">
                  <span className="feed-dot" aria-hidden />
                  <span className="feed-text">
                    {t.publisher.nickname} {t.status === 'COMPLETED' ? '完成了' : '发布了'}辅导
                  </span>
                  <span className="feed-time">{formatRelativeTime(t.createdAt)}</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <SafetyCard />
      </aside>
    </div>
  )
}
