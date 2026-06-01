import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { searchItems } from '../../api/trade'
import type { TradeItemStatus } from '../../types/trade'
import TradeCard from '../../components/domain/TradeCard'
import Select from '../../components/Select'
import { StatIcon, SafetyCard } from '../../components/HallWidgets'
import { formatRelativeTime } from '../../utils/format'
import '../tasks/Tasks.css'
import './Trade.css'

const STATUS_OPTS = [
  { value: 'ON_SALE', label: '在售' },
  { value: 'ALL', label: '全部状态' },
  { value: 'IN_TRADE', label: '交易中' },
  { value: 'OFF_SALE', label: '已下架' },
] as const

const SORT_OPTS = [
  { value: 'latest', label: '最新发布' },
  { value: 'cheap', label: '价格最低' },
] as const

type SortKey = 'latest' | 'cheap'

export default function TradeHallPage() {
  const [status, setStatus] = useState<TradeItemStatus | null>('ON_SALE')
  const [q, setQ] = useState('')
  const [sort, setSort] = useState<SortKey>('latest')

  const { data, isLoading } = useQuery({
    queryKey: ['items', { status, q }],
    queryFn: () => searchItems({ status: status ?? undefined, q: q || undefined, page: 1, size: 16 }),
  })

  const items = useMemo(() => {
    if (!data) return []
    const arr = [...data.items]
    arr.sort((a, b) => sort === 'cheap'
      ? a.pricePoint - b.pricePoint
      : new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    return arr
  }, [data, sort])

  const { data: stats } = useQuery({
    queryKey: ['items', 'stats'],
    staleTime: 60_000,
    queryFn: async () => {
      const [onSale, inTrade, offSale] = await Promise.all([
        searchItems({ status: 'ON_SALE', page: 1, size: 1 }),
        searchItems({ status: 'IN_TRADE', page: 1, size: 1 }),
        searchItems({ status: 'OFF_SALE', page: 1, size: 1 }),
      ])
      return { onSale: onSale.total, inTrade: inTrade.total, offSale: offSale.total }
    },
  })

  const { data: recent } = useQuery({
    queryKey: ['items', 'recent'],
    staleTime: 30_000,
    queryFn: () => searchItems({ page: 1, size: 8 }),
  })
  const recentItems = recent?.items ?? []

  const STAT_CARDS = [
    { num: stats?.onSale, label: '在售好物', icon: 'tag' as const },
    { num: stats?.inTrade, label: '交易中', icon: 'cart' as const },
    { num: stats?.offSale, label: '已成交', icon: 'check' as const },
  ]

  return (
    <div className="wrap hall-grid">
      <div className="hall-main">
        <section className="hall-hero">
          <div className="hall-hero-lead">
            <h1 className="page-title">
              宿舍里的<span className="it">好东西</span>。
            </h1>
            <p className="hall-hero-sub">同校面交、积分结算，把闲置流转给需要的人。</p>
            <div className="hall-cta">
              <a href="#hall-list" className="hall-cta-primary">逛逛二手 <span aria-hidden>→</span></a>
              <Link to="/app/trade/new" className="hall-cta-ghost">挂个出售 <span aria-hidden>+</span></Link>
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
            <img className="hall-illo" src="/illustrations/coffee.png" alt="" />
          </div>
        </section>

        <div className="task-toolbar" id="hall-list">
          <Select
            value={status ?? 'ALL'}
            options={STATUS_OPTS}
            onChange={(v) => setStatus(v === 'ALL' ? null : v as TradeItemStatus)}
            ariaLabel="状态筛选"
          />
          <input
            type="search"
            className="task-search"
            placeholder="搜索：Kindle / 教材 / 电饭锅…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
          <div className="toolbar-spacer" />
          <Select value={sort} options={SORT_OPTS} onChange={(v) => setSort(v as SortKey)} ariaLabel="排序" />
        </div>

        {isLoading && <div className="task-loading">正在加载…</div>}

        {data && items.length === 0 && (
          <div className="task-empty">
            没<span className="it">货</span>了。 换个筛选试试。
          </div>
        )}

        {items.length > 0 && (
          <div className="trade-grid">
            {items.map((item) => (
              <TradeCard key={item.id} item={item} />
            ))}
          </div>
        )}
      </div>

      <aside className="hall-aside">
        <section className="aside-card">
          <div className="aside-head">
            <h2 className="aside-title">二手动态</h2>
          </div>
          {recentItems.length === 0 ? (
            <div className="aside-empty">暂无动态</div>
          ) : (
            <ul className="feed-list">
              {recentItems.slice(0, 6).map((it) => (
                <li key={it.id} className="feed-item">
                  <span className="feed-dot" aria-hidden />
                  <span className="feed-text">
                    {it.seller.nickname} 上架了 {it.title}
                  </span>
                  <span className="feed-time">{formatRelativeTime(it.createdAt)}</span>
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
