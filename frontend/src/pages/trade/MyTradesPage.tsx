import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  listMyOffers, listMyOrders,
  acceptOffer, rejectOffer, cancelOffer, counterOffer,
  confirmOrder, cancelOrder,
} from '../../api/trade'
import { useAuthStore } from '../../stores/auth'
import { MOCK_CURRENT_USER_ID } from '../../api/_mock'
import type { TradeOfferStatus, TradeOrderStatus } from '../../types/trade'
import { formatRelativeTime } from '../../utils/format'
import '../tasks/Tasks.css'
import './Trade.css'

type Tab = 'buy' | 'sell'

const OFFER_LABEL: Record<TradeOfferStatus, { text: string; tone: string }> = {
  PENDING:  { text: '议价中', tone: 'pending' },
  ACCEPTED: { text: '已成交', tone: 'done' },
  REJECTED: { text: '已拒绝', tone: 'canceled' },
  CANCELED: { text: '已撤回', tone: 'canceled' },
}

const ORDER_LABEL: Record<TradeOrderStatus, { text: string; tone: string }> = {
  IN_TRADE:        { text: '交易中', tone: 'progress' },
  BUYER_CONFIRMED: { text: '买家已确认', tone: 'wait' },
  SELLER_CONFIRMED:{ text: '卖家已确认', tone: 'wait' },
  COMPLETED:       { text: '已完成', tone: 'done' },
  CANCELED:        { text: '已取消', tone: 'canceled' },
}

export default function MyTradesPage() {
  const qc = useQueryClient()
  const myId = useAuthStore((s) => s.userId) ?? MOCK_CURRENT_USER_ID
  const meNum = Number(myId)
  const [tab, setTab] = useState<Tab>('buy')
  const [counterId, setCounterId] = useState<number | null>(null)
  const [counterPrice, setCounterPrice] = useState('')

  const { data: offers } = useQuery({ queryKey: ['trade', 'my-offers'], queryFn: listMyOffers })
  const { data: orders } = useQuery({ queryKey: ['trade', 'my-orders'], queryFn: listMyOrders })

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['trade', 'my-offers'] })
    qc.invalidateQueries({ queryKey: ['trade', 'my-orders'] })
    qc.invalidateQueries({ queryKey: ['item'] })
  }

  const acceptM = useMutation({ mutationFn: (id: number) => acceptOffer(id), onSuccess: invalidate })
  const rejectM = useMutation({ mutationFn: (id: number) => rejectOffer(id), onSuccess: invalidate })
  const cancelOfferM = useMutation({ mutationFn: (id: number) => cancelOffer(id), onSuccess: invalidate })
  const counterM = useMutation({
    mutationFn: ({ id, price }: { id: number; price: number }) => counterOffer(id, price),
    onSuccess: () => { setCounterId(null); invalidate() },
  })
  const confirmM = useMutation({ mutationFn: (id: number) => confirmOrder(id), onSuccess: invalidate })
  const cancelOrderM = useMutation({ mutationFn: (id: number) => cancelOrder(id), onSuccess: invalidate })

  const offerBusy = acceptM.isPending || rejectM.isPending || cancelOfferM.isPending || counterM.isPending
  const orderBusy = confirmM.isPending || cancelOrderM.isPending

  const myOffers = (offers ?? []).filter((o) => (tab === 'buy' ? o.isBuyer : !o.isBuyer))
  const myOrders = (orders ?? []).filter((o) => (tab === 'buy' ? o.buyerId === meNum : o.sellerId === meNum))

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">我的<span className="it">交易</span>。</h1>
        <div className="page-sub">砍价 · 订单 · 确认收货</div>
      </div>

      <div className="my-tabs">
        <button type="button" onClick={() => setTab('buy')} className={`my-tab${tab === 'buy' ? ' is-active' : ''}`}>
          我<span className="it">买</span>的
        </button>
        <button type="button" onClick={() => setTab('sell')} className={`my-tab${tab === 'sell' ? ' is-active' : ''}`}>
          我<span className="it">卖</span>的
        </button>
      </div>

      {/* ── 砍价 ── */}
      <h2 className="trade-mine-section">议价</h2>
      {myOffers.length === 0 ? (
        <div className="task-empty">暂时没有<span className="it">议价。</span></div>
      ) : (
        <ul className="trade-mine-list">
          {myOffers.map((o) => {
            const meta = OFFER_LABEL[o.status]
            return (
              <li key={o.id} className="trade-mine-card">
                <div className="trade-mine-main">
                  <Link to={`/app/trade/${o.itemId}`} className="trade-mine-title">{o.itemTitle}</Link>
                  <div className="trade-mine-sub">
                    出价 <b>{o.pricePoint}</b> 积分 · 标价 {o.itemPricePoint}
                    {o.status === 'PENDING' && (
                      <> · {o.myTurn ? '轮到你' : '等待对方'}</>
                    )}
                  </div>
                  <span className="trade-mine-time">{formatRelativeTime(o.updatedAt)}</span>
                </div>
                <div className="trade-mine-right">
                  <span className={`status-badge status-${meta.tone}`}>{meta.text}</span>
                  {o.status === 'PENDING' && (
                    <div className="trade-mine-acts">
                      {o.myTurn && (
                        <>
                          <button className="mini-btn mini-btn-primary" disabled={offerBusy}
                            onClick={() => acceptM.mutate(o.id)}>同意</button>
                          <button className="mini-btn" disabled={offerBusy}
                            onClick={() => { setCounterId(o.id); setCounterPrice(String(o.pricePoint)) }}>还价</button>
                          <button className="mini-btn" disabled={offerBusy}
                            onClick={() => rejectM.mutate(o.id)}>拒绝</button>
                        </>
                      )}
                      {o.isBuyer && (
                        <button className="mini-btn" disabled={offerBusy}
                          onClick={() => cancelOfferM.mutate(o.id)}>撤回</button>
                      )}
                    </div>
                  )}
                  {o.status === 'ACCEPTED' && o.orderId && (
                    <span className="trade-mine-hint">已生成订单，见下方</span>
                  )}
                </div>
                {counterId === o.id && (
                  <div className="trade-offer-form trade-mine-counter">
                    <div className="trade-offer-input-row">
                      <input className="trade-offer-input" type="number" min={1}
                        value={counterPrice} onChange={(e) => setCounterPrice(e.target.value)} placeholder="还价" />
                      <span className="trade-offer-unit">积分</span>
                    </div>
                    <div className="trade-offer-actions">
                      <button className="action-btn action-btn-primary"
                        disabled={!Number(counterPrice) || counterM.isPending}
                        onClick={() => counterM.mutate({ id: o.id, price: Number(counterPrice) })}>
                        {counterM.isPending ? '提交中…' : '提交还价'}
                      </button>
                      <button className="action-btn action-btn-ghost" onClick={() => setCounterId(null)}>取消</button>
                    </div>
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}

      {/* ── 订单 ── */}
      <h2 className="trade-mine-section">订单</h2>
      {myOrders.length === 0 ? (
        <div className="task-empty">暂时没有<span className="it">订单。</span></div>
      ) : (
        <ul className="trade-mine-list">
          {myOrders.map((o) => {
            const meta = ORDER_LABEL[o.status]
            const iAmBuyer = o.buyerId === meNum
            const iConfirmed = iAmBuyer ? o.buyerConfirmed : o.sellerConfirmed
            const open = o.status !== 'COMPLETED' && o.status !== 'CANCELED'
            return (
              <li key={o.id} className="trade-mine-card">
                <div className="trade-mine-main">
                  <Link to={`/app/trade/${o.itemId}`} className="trade-mine-title">商品 #{o.itemId}</Link>
                  <div className="trade-mine-sub">成交 <b>{o.negotiatedPricePoint}</b> 积分 · 押金 {o.freezePoint}</div>
                  <span className="trade-mine-time">{formatRelativeTime(o.createdAt)}</span>
                </div>
                <div className="trade-mine-right">
                  <span className={`status-badge status-${meta.tone}`}>{meta.text}</span>
                  {open && (
                    <div className="trade-mine-acts">
                      <button className="mini-btn mini-btn-primary" disabled={orderBusy || iConfirmed}
                        onClick={() => confirmM.mutate(o.id)}>
                        {iConfirmed ? '已确认' : iAmBuyer ? '确认收货' : '确认发货'}
                      </button>
                      <button className="mini-btn" disabled={orderBusy}
                        onClick={() => cancelOrderM.mutate(o.id)}>取消</button>
                    </div>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
