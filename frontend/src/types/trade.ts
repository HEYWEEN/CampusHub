/**
 * 二手交易相关 VO / DTO — 与后端 trade/vo + dto 字段名严格对齐
 * （schema_audit A-3 / A-4 / B-9 修复）
 */
import type { PublicUserVO } from './user'

// 对齐后端 TradeItemStatus enum
export type TradeItemStatus = 'ON_SALE' | 'OFF_SALE' | 'IN_TRADE'

// 对齐后端 PickupLocationType enum
export type PickupLocationType = 'EXACT_DORM' | 'BUILDING_RANGE' | 'MEETUP'

// 对齐后端 TradeItemVO record
export interface TradeItemVO {
  id: number                       // 后端 Long（B-5 ID 类型留 P3 全局解决）
  seller: PublicUserVO
  title: string
  description: string
  pricePoint: number               // 原前端 price
  pickupLocationType: PickupLocationType
  pickupLocationDetail: string     // 原前端 buildingRange?
  status: TradeItemStatus
  imageUrls: string[]              // 原前端 images
  createdAt: string
}

// 对齐后端 TradeItemCreateDTO（已改 JSON + imageUrls）
export interface TradeItemCreateDTO {
  title: string
  description?: string
  pricePoint: number
  pickupLocationType: PickupLocationType
  pickupLocationDetail?: string
  imageUrls: string[]              // 由前端预先调 POST /api/uploads 拿到
}

// 对齐后端 TradeItemQueryDTO
export interface TradeSearchParams {
  page?: number
  size?: number
  status?: TradeItemStatus
  q?: string
}

// ==================== 订单 ====================

// 对齐后端 TradeOrderStatus enum
export type TradeOrderStatus =
  | 'IN_TRADE' | 'BUYER_CONFIRMED' | 'SELLER_CONFIRMED' | 'COMPLETED' | 'CANCELED'

// 对齐后端 TradeOrderVO record
export interface TradeOrderVO {
  id: number
  itemId: number
  buyerId: number
  sellerId: number
  status: TradeOrderStatus
  negotiatedPricePoint: number
  freezePoint: number
  buyerConfirmed: boolean
  sellerConfirmed: boolean
  createdAt: string
}

export interface TradeOrderCreateDTO {
  itemId: number
  negotiatedPricePoint: number
}

// ==================== 砍价 ====================

// 对齐后端 TradeOfferStatus enum
export type TradeOfferStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELED'
export type AwaitingRole = 'BUYER' | 'SELLER'

// 对齐后端 TradeOfferVO record
export interface TradeOfferVO {
  id: number
  itemId: number
  itemTitle: string
  itemPricePoint: number
  buyer: PublicUserVO
  seller: PublicUserVO
  pricePoint: number
  status: TradeOfferStatus
  awaitingRole: AwaitingRole
  isBuyer: boolean       // 当前用户是买家
  myTurn: boolean        // 当前用户是该回合方（能 同意/还价/拒绝）
  orderId: number | null
  createdAt: string
  updatedAt: string
}

export interface TradeOfferCreateDTO {
  itemId: number
  pricePoint: number
}

export interface TradeOfferPriceDTO {
  pricePoint: number
}
