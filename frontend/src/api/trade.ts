import { apiGet, apiPost } from './client'
import { type PageResponse } from '../types/api'
import type {
  TradeItemCreateDTO, TradeItemVO, TradeSearchParams,
  TradeOrderVO, TradeOfferVO,
} from '../types/trade'
import {
  mockCreateItem, mockGetItem, mockSearchItems,
  mockCreateOrder, mockConfirmOrder, mockCancelOrder, mockListMyOrders,
  mockCreateOffer, mockCounterOffer, mockAcceptOffer, mockRejectOffer, mockCancelOffer, mockListMyOffers,
} from './_mock'
import { withMock } from './withMock'

export const searchItems = (params: TradeSearchParams) =>
  withMock<PageResponse<TradeItemVO>>(
    () => apiGet('/api/search/items', params),
    () => mockSearchItems(params),
    'trade',
  )

export const getItem = (itemId: number | string) =>
  withMock<TradeItemVO>(
    () => apiGet(`/api/trade/items/${itemId}`),
    () => mockGetItem(String(itemId)),
    'trade',
  )

// 后端 POST /api/trade/items 返回完整 TradeItemVO（schema_audit A-3/A-4 修复后从 multipart 改 JSON）
export const createItem = (dto: TradeItemCreateDTO) =>
  withMock<TradeItemVO>(
    () => apiPost('/api/trade/items', dto),
    () => mockCreateItem(dto),
    'trade',
  )

// ==================== 订单 ====================

/** 立即购买（按标价或协商价直接下单，冻结买家押金）。 */
export const createOrder = (itemId: number, negotiatedPricePoint: number) =>
  withMock<TradeOrderVO>(
    () => apiPost('/api/trade/orders', { itemId, negotiatedPricePoint }),
    () => mockCreateOrder(itemId, negotiatedPricePoint),
    'trade',
  )

export const confirmOrder = (orderId: number) =>
  withMock<TradeOrderVO>(
    () => apiPost(`/api/trade/orders/${orderId}/confirm`, {}),
    () => mockConfirmOrder(orderId),
    'trade',
  )

export const cancelOrder = (orderId: number) =>
  withMock<TradeOrderVO>(
    () => apiPost(`/api/trade/orders/${orderId}/cancel`, {}),
    () => mockCancelOrder(orderId),
    'trade',
  )

export const listMyOrders = () =>
  withMock<TradeOrderVO[]>(
    () => apiGet('/api/trade/orders/mine'),
    () => mockListMyOrders(),
    'trade',
  )

// ==================== 砍价 ====================

/** 买家出价发起砍价。 */
export const createOffer = (itemId: number, pricePoint: number) =>
  withMock<TradeOfferVO>(
    () => apiPost('/api/trade/offers', { itemId, pricePoint }),
    () => mockCreateOffer(itemId, pricePoint),
    'trade',
  )

export const counterOffer = (offerId: number, pricePoint: number) =>
  withMock<TradeOfferVO>(
    () => apiPost(`/api/trade/offers/${offerId}/counter`, { pricePoint }),
    () => mockCounterOffer(offerId, pricePoint),
    'trade',
  )

export const acceptOffer = (offerId: number) =>
  withMock<TradeOfferVO>(
    () => apiPost(`/api/trade/offers/${offerId}/accept`, {}),
    () => mockAcceptOffer(offerId),
    'trade',
  )

export const rejectOffer = (offerId: number) =>
  withMock<TradeOfferVO>(
    () => apiPost(`/api/trade/offers/${offerId}/reject`, {}),
    () => mockRejectOffer(offerId),
    'trade',
  )

export const cancelOffer = (offerId: number) =>
  withMock<TradeOfferVO>(
    () => apiPost(`/api/trade/offers/${offerId}/cancel`, {}),
    () => mockCancelOffer(offerId),
    'trade',
  )

export const listMyOffers = () =>
  withMock<TradeOfferVO[]>(
    () => apiGet('/api/trade/offers/mine'),
    () => mockListMyOffers(),
    'trade',
  )
