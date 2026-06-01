import { apiGet, apiPost } from './client'
import { type PageResponse } from '../types/api'
import type { TradeItemCreateDTO, TradeItemVO, TradeSearchParams } from '../types/trade'
import { mockCreateItem, mockGetItem, mockSearchItems } from './_mock'
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
