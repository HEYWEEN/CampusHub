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
