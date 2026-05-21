/**
 * 二手交易相关 VO / DTO
 */
import type { PublicUserVO } from './user'

export type TradeStatus = 'ON_SALE' | 'IN_TRADE' | 'COMPLETED' | 'WITHDRAWN'
export type PickupType = 'EXACT_DORM' | 'BUILDING_RANGE' | 'MEETING'

export interface TradeItemVO {
  itemId: string
  title: string
  price: number             // 积分价格
  description: string
  images: string[]
  pickupType: PickupType
  buildingRange?: string
  status: TradeStatus
  seller: PublicUserVO
  createdAt: string
}

export interface TradeItemCreateDTO {
  title: string
  price: number
  description: string
  images: string[]
  pickupType: PickupType
  buildingRange?: string
}

export interface TradeSearchParams {
  page?: number
  size?: number
  status?: TradeStatus
  q?: string
}
