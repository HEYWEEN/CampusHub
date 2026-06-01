import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { searchTasks } from '../../api/task'
import type { TaskListItemVO, TaskStatus, TaskType } from '../../types/task'
import TaskCard from '../../components/domain/TaskCard'
import Select from '../../components/Select'
import { StatIcon, SafetyCard } from '../../components/HallWidgets'
import { TASK_TYPE_LABEL } from '../../utils/labels'
import { formatRelativeTime } from '../../utils/format'
import './Tasks.css'

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

// 辅导(TUTOR) 归 edu 模块（/app/edu/tutor），任务大厅只做跑腿/互助需求
const TYPES: { value: TaskType | null; label: string }[] = [
  { value: null, label: '全部' },
  { value: 'ERRAND', label: '跑腿' },
  { value: 'MUTUAL_HELP', label: '互助' },
]

type SortKey = 'latest' | 'reward'

/** 校园动态：把一条任务渲染成「谁 做了什么」。 */
function feedVerb(t: TaskListItemVO): string {
  if (t.status === 'COMPLETED') return '完成了'
  if (t.status === 'PENDING_ACCEPT') return '发布了'
  return '更新了'
}

export default function TaskHallPage() {
  const [type, setType] = useState<TaskType | null>(null)
  const [status, setStatus] = useState<TaskStatus | null>('PENDING_ACCEPT')
  const [q, setQ] = useState('')
  const [sort, setSort] = useState<SortKey>('latest')

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

  const items = useMemo(() => {
    if (!data) return []
    const arr = [...data.items]
    arr.sort((a, b) => sort === 'reward'
      ? b.rewardPoint - a.rewardPoint
      : new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    return arr
  }, [data, sort])

  // 顶部统计：三个状态各取 total（size=1 轻量计数，并行）
  const { data: stats } = useQuery({
    queryKey: ['tasks', 'stats'],
    staleTime: 60_000,
    queryFn: async () => {
      const [inProgress, pending, completed] = await Promise.all([
        searchTasks({ status: 'IN_PROGRESS', page: 1, size: 1 }),
        searchTasks({ status: 'PENDING_ACCEPT', page: 1, size: 1 }),
        searchTasks({ status: 'COMPLETED', page: 1, size: 1 }),
      ])
      return { inProgress: inProgress.total, pending: pending.total, completed: completed.total }
    },
  })

  // 最新动态 / 热门任务：共用一份「最新 8 条」数据
  const { data: recent } = useQuery({
    queryKey: ['tasks', 'recent'],
    staleTime: 30_000,
    queryFn: () => searchTasks({ page: 1, size: 8 }),
  })
  const recentItems = recent?.items ?? []

  const STAT_CARDS = [
    { num: stats?.inProgress, label: '进行中任务', icon: 'clipboard' as const },
    { num: stats?.pending, label: '待接单', icon: 'clock' as const },
    { num: stats?.completed, label: '已完成', icon: 'check' as const },
  ]

  return (
    <div className="wrap hall-grid">
      <div className="hall-main">
        {/* ---- Hero：标题 + CTA + 统计 + 插画 ---- */}
        <section className="hall-hero">
          <div className="hall-hero-lead">
            <h1 className="page-title">
              找点<span className="it">事</span>做。
            </h1>
            <p className="hall-hero-sub">发现校园里的需求，帮助同学，也让你的时间更有价值。</p>
            <div className="hall-cta">
              <Link to="/app/tasks/new" className="hall-cta-primary">发布任务 <span aria-hidden>+</span></Link>
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
            <img className="hall-illo" src="/illustrations/float.png" alt="" />
          </div>
        </section>

        {/* ---- 工具栏：类型 pill（单排）+ 状态/排序下拉 ---- */}
        <div className="task-toolbar" id="hall-list">
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
          <Select
            value={status ?? 'ALL'}
            options={STATUS_OPTS}
            onChange={(v) => setStatus(v === 'ALL' ? null : v as TaskStatus)}
            ariaLabel="状态筛选"
          />
          <input
            type="search"
            className="task-search"
            placeholder="搜索标题或备注…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
          <div className="toolbar-spacer" />
          <Select
            value={sort}
            options={SORT_OPTS}
            onChange={(v) => setSort(v as SortKey)}
            ariaLabel="排序"
          />
        </div>

        {isLoading && <div className="task-loading">正在加载…</div>}

        {error && (
          <div className="task-error">
            加载失败：{(error as Error).message}
          </div>
        )}

        {data && items.length === 0 && (
          <div className="task-empty">
            这里很<span className="it">安静。</span> 当前筛选下没有任务。
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

      {/* ---- 右侧信息栏 ---- */}
      <aside className="hall-aside">
        <section className="aside-card">
          <div className="aside-head">
            <h2 className="aside-title">校园动态</h2>
          </div>
          {recentItems.length === 0 ? (
            <div className="aside-empty">暂无动态</div>
          ) : (
            <ul className="feed-list">
              {recentItems.slice(0, 6).map((t) => (
                <li key={t.taskId} className="feed-item">
                  <span className="feed-dot" aria-hidden />
                  <span className="feed-text">
                    {t.publisher.nickname} {feedVerb(t)} {TASK_TYPE_LABEL[t.taskType]}任务
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
