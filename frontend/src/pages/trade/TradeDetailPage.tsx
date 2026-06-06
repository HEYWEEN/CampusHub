import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getItem, createOrder, createOffer, listMyOffers } from '../../api/trade'
import { useAuthStore } from '../../stores/auth'
import { MOCK_CURRENT_USER_ID } from '../../api/_mock'
import { BizError } from '../../types/api'
import PublicUserCard from '../../components/domain/PublicUserCard'
import OfferActions from '../../components/domain/OfferActions'
import { useSendOrderCard } from '../../components/domain/useSendOrderCard'
import { formatRelativeTime } from '../../utils/format'
import { TRADE_STATUS_LABEL } from '../../utils/labels'
import '../tasks/Tasks.css'
import './Trade.css'

const PICKUP_LABEL: Record<string, string> = {
  EXACT_DORM: '精确宿舍',
  BUILDING_RANGE: '楼栋范围',
  MEETUP: '面交',
}

export default function TradeDetailPage() {
  const { id = '' } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const myId = useAuthStore((s) => s.userId) ?? MOCK_CURRENT_USER_ID

  const { data: item, isLoading, error } = useQuery({
    queryKey: ['item', id],
    queryFn: () => getItem(id),
    enabled: !!id,
  })
  // 本商品上「我参与的、进行中的」报价（卖家看到买家出价、买家看到自己出价）
  const { data: myOffers } = useQuery({ queryKey: ['trade', 'my-offers'], queryFn: listMyOffers })
  const itemOffers = (myOffers ?? []).filter((o) => o.itemId === Number(id) && o.status === 'PENDING')
  const onOfferDone = () => {
    qc.invalidateQueries({ queryKey: ['trade', 'my-offers'] })
    qc.invalidateQueries({ queryKey: ['item', id] })
  }
  const [showOffer, setShowOffer] = useState(false)
  const [offerPrice, setOfferPrice] = useState('')
  const [actErr, setActErr] = useState('')
  const sendOrder = useSendOrderCard((m) => setActErr(m))

  const buyMut = useMutation({
    mutationFn: (price: number) => createOrder(Number(id), price),
    onSuccess: () => navigate('/app/trade/mine'),
    onError: (e) => setActErr(e instanceof BizError ? e.message : '下单失败'),
  })
  const offerMut = useMutation({
    mutationFn: (price: number) => createOffer(Number(id), price),
    onSuccess: () => navigate('/app/trade/mine'),
    onError: (e) => setActErr(e instanceof BizError ? e.message : '出价失败'),
  })

  if (isLoading) return <div className="wrap"><div className="task-loading">加载中…</div></div>
  if (error || !item) {
    return <div className="wrap"><div className="task-error">{(error as Error)?.message ?? '商品不存在'}</div></div>
  }

  const status = TRADE_STATUS_LABEL[item.status]
  const isSeller = item.seller.userId === myId
  const myOffer = itemOffers.find((o) => o.isBuyer)   // 买家视角：我对本商品的进行中报价

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
            <>
              <button className="action-btn action-btn-ghost" disabled>
                这是你发布的商品
              </button>
              {itemOffers.length > 0 && (
                <div className="trade-offers-box">
                  <div className="trade-offers-title">收到的议价 · {itemOffers.length}</div>
                  {itemOffers.map((o) => (
                    <div key={o.id} className="trade-offer-item">
                      <div className="trade-offer-item-info">
                        <PublicUserCard user={o.buyer} size="sm" />
                        <span className="trade-offer-item-price"><b>{o.pricePoint}</b> 积分</span>
                      </div>
                      <OfferActions offer={o} onDone={onOfferDone} />
                    </div>
                  ))}
                </div>
              )}
            </>
          ) : item.status === 'ON_SALE' ? (
            <>
              <button
                className="action-btn action-btn-primary"
                disabled={buyMut.isPending}
                onClick={() => { setActErr(''); buyMut.mutate(item.pricePoint) }}
              >
                {buyMut.isPending ? '下单中…' : `立即购买 · ${item.pricePoint} 积分`}
              </button>

              {myOffer ? (
                <div className="trade-offers-box">
                  <div className="trade-offers-title">
                    你的议价 · {myOffer.pricePoint} 积分（{myOffer.myTurn ? '轮到你' : '等待卖家回应'}）
                  </div>
                  <OfferActions offer={myOffer} onDone={onOfferDone} />
                </div>
              ) : !showOffer ? (
                <button
                  className="action-btn action-btn-ghost"
                  onClick={() => { setActErr(''); setOfferPrice(String(item.pricePoint)); setShowOffer(true) }}
                >
                  出价砍价
                </button>
              ) : (
                <div className="trade-offer-form">
                  <div className="trade-offer-input-row">
                    <input
                      className="trade-offer-input"
                      type="number"
                      min={1}
                      value={offerPrice}
                      onChange={(e) => setOfferPrice(e.target.value)}
                      placeholder="你的出价"
                    />
                    <span className="trade-offer-unit">积分</span>
                  </div>
                  <div className="trade-offer-actions">
                    <button
                      className="action-btn action-btn-primary"
                      disabled={!Number(offerPrice) || offerMut.isPending}
                      onClick={() => { setActErr(''); offerMut.mutate(Number(offerPrice)) }}
                    >
                      {offerMut.isPending ? '提交中…' : '提交出价'}
                    </button>
                    <button className="action-btn action-btn-ghost" onClick={() => setShowOffer(false)}>
                      取消
                    </button>
                  </div>
                </div>
              )}

              <button
                className="action-btn action-btn-ghost"
                disabled={sendOrder.isPending}
                onClick={() => { setActErr(''); sendOrder.mutate({
                  peerId: item.seller.userId,
                  card: {
                    kind: 'TRADE',
                    id: item.id,
                    title: item.title,
                    cover: item.imageUrls[0] ?? null,
                    pricePoint: item.pricePoint,
                  },
                }) }}
              >
                {sendOrder.isPending ? '发起中…' : '联系卖家 · 随便聊聊'}
              </button>

              {actErr && <p className="form-error">{actErr}</p>}
              <p className="action-hint">
                立即购买/出价成交后会冻结相应积分作为押金，在「我的交易」里确认收货
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
