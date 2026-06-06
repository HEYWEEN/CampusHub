import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { acceptOffer, rejectOffer, cancelOffer, counterOffer } from '../../api/trade'
import type { TradeOfferVO } from '../../types/trade'

/**
 * 砍价操作按钮组（同意 / 还价 / 拒绝 / 撤回 + 内联还价表单）。
 * 详情页与「我的交易」共用。仅 PENDING 时渲染。
 *
 * @param offer  当前报价
 * @param onDone 任一操作成功后回调（调用方负责 invalidate 相关 query）
 */
export default function OfferActions({ offer, onDone }: { offer: TradeOfferVO; onDone: () => void }) {
  const [counterOpen, setCounterOpen] = useState(false)
  const [price, setPrice] = useState('')

  const acceptM = useMutation({ mutationFn: () => acceptOffer(offer.id), onSuccess: onDone })
  const rejectM = useMutation({ mutationFn: () => rejectOffer(offer.id), onSuccess: onDone })
  const cancelM = useMutation({ mutationFn: () => cancelOffer(offer.id), onSuccess: onDone })
  const counterM = useMutation({
    mutationFn: (p: number) => counterOffer(offer.id, p),
    onSuccess: () => { setCounterOpen(false); onDone() },
  })
  const busy = acceptM.isPending || rejectM.isPending || cancelM.isPending || counterM.isPending

  if (offer.status !== 'PENDING') return null

  return (
    <div className="offer-acts-wrap">
      <div className="trade-mine-acts">
        {offer.myTurn && (
          <>
            <button className="mini-btn mini-btn-primary" disabled={busy} onClick={() => acceptM.mutate()}>
              同意
            </button>
            <button className="mini-btn" disabled={busy}
              onClick={() => { setPrice(String(offer.pricePoint)); setCounterOpen(true) }}>
              还价
            </button>
            <button className="mini-btn" disabled={busy} onClick={() => rejectM.mutate()}>
              拒绝
            </button>
          </>
        )}
        {offer.isBuyer && (
          <button className="mini-btn" disabled={busy} onClick={() => cancelM.mutate()}>撤回</button>
        )}
        {!offer.myTurn && !offer.isBuyer && (
          <span className="trade-mine-hint">等待对方回应</span>
        )}
      </div>

      {counterOpen && (
        <div className="trade-offer-form trade-mine-counter">
          <div className="trade-offer-input-row">
            <input className="trade-offer-input" type="number" min={1}
              value={price} onChange={(e) => setPrice(e.target.value)} placeholder="还价" />
            <span className="trade-offer-unit">积分</span>
          </div>
          <div className="trade-offer-actions">
            <button className="action-btn action-btn-primary"
              disabled={!Number(price) || counterM.isPending}
              onClick={() => counterM.mutate(Number(price))}>
              {counterM.isPending ? '提交中…' : '提交还价'}
            </button>
            <button className="action-btn action-btn-ghost" onClick={() => setCounterOpen(false)}>取消</button>
          </div>
        </div>
      )}
    </div>
  )
}
