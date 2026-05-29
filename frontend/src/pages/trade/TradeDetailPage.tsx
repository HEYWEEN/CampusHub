import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getItem } from '../../api/trade'
import { useAuthStore } from '../../stores/auth'
import { MOCK_CURRENT_USER_ID } from '../../api/_mock'
import PublicUserCard from '../../components/domain/PublicUserCard'
import { formatRelativeTime } from '../../utils/format'
import '../tasks/Tasks.css'
import './Trade.css'

const PICKUP_LABEL: Record<string, string> = {
  EXACT_DORM: '精确宿舍',
  BUILDING_RANGE: '楼栋范围',
  MEETUP: '面交',
}

const STATUS_LABEL = {
  ON_SALE:  { text: '在售',   tone: 'pending' as const },
  IN_TRADE: { text: '交易中', tone: 'progress' as const },
  OFF_SALE: { text: '已下架', tone: 'canceled' as const },
}

export default function TradeDetailPage() {
  const { id = '' } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const myId = useAuthStore((s) => s.userId) ?? MOCK_CURRENT_USER_ID

  const { data: item, isLoading, error } = useQuery({
    queryKey: ['item', id],
    queryFn: () => getItem(id),
    enabled: !!id,
  })

  if (isLoading) return <div className="wrap"><div className="task-loading">加载中…</div></div>
  if (error || !item) {
    return <div className="wrap"><div className="task-error">{(error as Error)?.message ?? '商品不存在'}</div></div>
  }

  const status = STATUS_LABEL[item.status]
  const isSeller = item.seller.userId === myId

  return (
    <div className="wrap">
      <button onClick={() => navigate(-1)} className="detail-back">
        <span aria-hidden>←</span> 返回二手大厅
      </button>

      <div className="trade-detail-layout">
        {/* 左：图片画廊（简化为单张） */}
        <div className="trade-gallery">
          <span className={`status-badge status-${status.tone} trade-gallery-status`}>
            {status.text}
          </span>
          <img src={item.imageUrls[0]} alt={item.title} />
        </div>

        {/* 右：信息 + 操作 */}
        <aside className="trade-detail-side">
          <h1 className="trade-detail-title">{item.title}</h1>
          <div className="trade-detail-price">
            {item.pricePoint}<span className="trade-detail-price-unit">积分</span>
          </div>

          <div>
            <div className="trade-meta-row">
              <span className="trade-meta-key">卖家</span>
              <PublicUserCard user={item.seller} size="sm" link />
            </div>
            <div className="trade-meta-row">
              <span className="trade-meta-key">取货方式</span>
              <span className="trade-meta-val">{PICKUP_LABEL[item.pickupLocationType]}</span>
            </div>
            {item.pickupLocationDetail && (
              <div className="trade-meta-row">
                <span className="trade-meta-key">地点</span>
                <span className="trade-meta-val">{item.pickupLocationDetail}</span>
              </div>
            )}
            <div className="trade-meta-row">
              <span className="trade-meta-key">发布于</span>
              <span className="trade-meta-val">{formatRelativeTime(item.createdAt)}</span>
            </div>
          </div>

          <p className="trade-detail-desc">{item.description}</p>

          {/* 操作按钮 */}
          {isSeller ? (
            <button className="action-btn action-btn-ghost" disabled>
              这是你发布的商品
            </button>
          ) : item.status === 'ON_SALE' ? (
            <>
              <button className="action-btn action-btn-primary">
                联系卖家 · 议价 →
              </button>
              <p className="action-hint">
                议价 / 下单都通过私信完成，下单后会冻结你 {item.pricePoint} 积分作为押金
              </p>
            </>
          ) : (
            <button className="action-btn action-btn-ghost" disabled>
              {status.text} · 暂时无法下单
            </button>
          )}
        </aside>
      </div>
    </div>
  )
}
