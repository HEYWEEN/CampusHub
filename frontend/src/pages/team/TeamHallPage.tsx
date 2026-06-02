import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { searchTeams } from '../../api/team'
import type { TeamRecruitStatus } from '../../types/team'
import TeamCard from '../../components/domain/TeamCard'
import Select from '../../components/Select'
import { StatIcon, SafetyCard } from '../../components/HallWidgets'
import { formatRelativeTime } from '../../utils/format'
import '../tasks/Tasks.css'

const STATUS_OPTS = [
  { value: 'RECRUITING', label: '招募中' },
  { value: 'ALL', label: '全部状态' },
  { value: 'FULL', label: '已满员' },
  { value: 'CLOSED', label: '已关闭' },
] as const

const SORT_OPTS = [
  { value: 'latest', label: '最新发布' },
  { value: 'open', label: '缺人最多' },
] as const

type SortKey = 'latest' | 'open'

export default function TeamHallPage() {
  const [status, setStatus] = useState<TeamRecruitStatus | null>('RECRUITING')
  const [q, setQ] = useState('')
  const [sort, setSort] = useState<SortKey>('latest')

  const { data, isLoading } = useQuery({
    queryKey: ['teams', { status, q }],
    queryFn: () => searchTeams({ status: status ?? undefined, q: q || undefined, page: 1, size: 12 }),
  })

  const items = useMemo(() => {
    if (!data) return []
    const arr = [...data.items]
    arr.sort((a, b) => sort === 'open'
      ? (b.totalSize - b.currentSize) - (a.totalSize - a.currentSize)
      : new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    return arr
  }, [data, sort])

  const { data: stats } = useQuery({
    queryKey: ['teams', 'stats'],
    staleTime: 60_000,
    queryFn: async () => {
      const [recruiting, full, closed] = await Promise.all([
        searchTeams({ status: 'RECRUITING', page: 1, size: 1 }),
        searchTeams({ status: 'FULL', page: 1, size: 1 }),
        searchTeams({ status: 'CLOSED', page: 1, size: 1 }),
      ])
      return { recruiting: recruiting.total, full: full.total, closed: closed.total }
    },
  })

  const { data: recent } = useQuery({
    queryKey: ['teams', 'recent'],
    staleTime: 30_000,
    queryFn: () => searchTeams({ page: 1, size: 8 }),
  })
  const recentItems = recent?.items ?? []

  const STAT_CARDS = [
    { num: stats?.recruiting, label: '招募中', icon: 'users' as const },
    { num: stats?.full, label: '已满员', icon: 'check' as const },
    { num: stats?.closed, label: '已结束', icon: 'flag' as const },
  ]

  return (
    <div className="wrap hall-grid">
      <div className="hall-main">
        <section className="hall-hero">
          <div className="hall-hero-lead">
            <h1 className="page-title">
              找几个<span className="it">队友</span>。
            </h1>
            <p className="hall-hero-sub">比赛、课设、跑步…说清要什么人，组一支靠谱的队。</p>
            <div className="hall-cta">
              <Link to="/app/team/new" className="hall-cta-primary">发布组队 <span aria-hidden>+</span></Link>
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
            <img className="hall-illo" src="/illustrations/catching-up.png" alt="" />
          </div>
        </section>

        <div className="task-toolbar" id="hall-list">
          <Select
            value={status ?? 'ALL'}
            options={STATUS_OPTS}
            onChange={(v) => setStatus(v === 'ALL' ? null : v as TeamRecruitStatus)}
            ariaLabel="状态筛选"
          />
          <input
            type="search"
            className="task-search"
            placeholder="搜索：数模 / 软工 / 篮球…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
          <div className="toolbar-spacer" />
          <Select value={sort} options={SORT_OPTS} onChange={(v) => setSort(v as SortKey)} ariaLabel="排序" />
        </div>

        {isLoading && <div className="task-loading">正在加载…</div>}

        {data && items.length === 0 && (
          <div className="task-empty">
            还没有<span className="it">组队。</span> 来发起第一支队伍。
          </div>
        )}

        {items.length > 0 && (
          <div className="tasks-grid">
            {items.map((r) => (
              <TeamCard key={r.recruitId} recruit={r} />
            ))}
          </div>
        )}
      </div>

      <aside className="hall-aside">
        <section className="aside-card">
          <div className="aside-head">
            <h2 className="aside-title">组队动态</h2>
          </div>
          {recentItems.length === 0 ? (
            <div className="aside-empty">暂无动态</div>
          ) : (
            <ul className="feed-list">
              {recentItems.slice(0, 6).map((r) => (
                <li key={r.recruitId} className="feed-item">
                  <span className="feed-dot" aria-hidden />
                  <span className="feed-text">{r.creator.nickname} 发起了组队</span>
                  <span className="feed-time">{formatRelativeTime(r.createdAt)}</span>
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
