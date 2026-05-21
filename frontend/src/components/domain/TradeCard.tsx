import { Link } from 'react-router-dom'
import type { TradeItemVO } from '../../types/trade'
import PublicUserCard from './PublicUserCard'
import './Domain.css'

const STATUS_LABEL: Record<TradeItemVO['status'], { text: string; tone: string }> = {
  ON_SALE:    { text: '在售', tone: 'pending' },
  IN_TRADE:   { text: '交易中', tone: 'progress' },
  COMPLETED:  { text: '已售出', tone: 'done' },
  WITHDRAWN:  { text: '已下架', tone: 'canceled' },
}

export default function TradeCard({ item }: { item: TradeItemVO }) {
  const status = STATUS_LABEL[item.status]
  return (
    <Link to={`/app/trade/${item.itemId}`} className="trade-card">
      <div className="trade-card-img">
        <img src={item.images[0]} alt={item.title} loading="lazy" />
        <span className={`status-badge status-${status.tone} trade-card-status`}>
          {status.text}
        </span>
      </div>
      <div className="trade-card-body">
        <h3 className="trade-card-title">{item.title}</h3>
        <div className="trade-card-foot">
          <PublicUserCard user={item.seller} size="sm" />
          <span className="trade-price">
            <span className="trade-price-num">{item.price}</span>
            <span className="trade-price-unit">积分</span>
          </span>
        </div>
      </div>
    </Link>
  )
}
