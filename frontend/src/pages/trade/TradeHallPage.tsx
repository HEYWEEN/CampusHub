import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { searchItems } from '../../api/trade'
import type { TradeItemStatus } from '../../types/trade'
import TradeCard from '../../components/domain/TradeCard'
import '../tasks/Tasks.css'
import './Trade.css'

const STATUSES: { value: TradeItemStatus | null; label: string }[] = [
  { value: 'ON_SALE',  label: '在售' },
  { value: null,       label: '全部' },
  { value: 'IN_TRADE', label: '交易中' },
  { value: 'OFF_SALE', label: '已下架' },
]

export default function TradeHallPage() {
  const [status, setStatus] = useState<TradeItemStatus | null>('ON_SALE')
  const [q, setQ] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['items', { status, q }],
    queryFn: () => searchItems({
      status: status ?? undefined,
      q: q || undefined,
      page: 1, size: 16,
    }),
  })

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">
          宿舍里的<span className="it">好东西</span>。
        </h1>
        <div className="page-sub">二手大厅 · 同校面交 · 积分结算</div>
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
          placeholder="搜索：Kindle / 教材 / 电饭锅…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <div className="toolbar-spacer" />
        <Link to="/app/trade/new" className="toolbar-new-btn">
          挂个出售 <span className="arr">→</span>
        </Link>
      </div>

      {isLoading && <div className="task-loading">正在加载…</div>}

      {data && data.items.length === 0 && (
        <div className="task-empty">
          没<span className="it">货</span>了。 换个筛选试试。
        </div>
      )}

      {data && data.items.length > 0 && (
        <div className="trade-grid">
          {data.items.map((item) => (
            <TradeCard key={item.id} item={item} />
          ))}
        </div>
      )}
    </div>
  )
}
