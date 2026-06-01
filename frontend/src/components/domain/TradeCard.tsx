import { Link } from 'react-router-dom'
import type { TradeItemVO } from '../../types/trade'
import PublicUserCard from './PublicUserCard'
import { TRADE_STATUS_LABEL } from '../../utils/labels'
import './Domain.css'

export default function TradeCard({ item }: { item: TradeItemVO }) {
  const status = TRADE_STATUS_LABEL[item.status]
  return (
    <Link to={`/app/trade/${item.id}`} className="trade-card">
      <div className="trade-card-img">
        <img src={item.imageUrls[0]} alt={item.title} loading="lazy" />
        <span className={`status-badge status-${status.tone} trade-card-status`}>
          {status.text}
        </span>
      </div>
      <div className="trade-card-body">
        <h3 className="trade-card-title">{item.title}</h3>
        <div className="trade-card-foot">
          <PublicUserCard user={item.seller} size="sm" />
          <span className="trade-price">
            <span className="trade-price-num">{item.pricePoint}</span>
            <span className="trade-price-unit">积分</span>
          </span>
        </div>
      </div>
    </Link>
  )
}
