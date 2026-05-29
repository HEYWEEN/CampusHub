import { apiGet, apiPost } from './client'
import { BizError, type PageResponse } from '../types/api'
import type { TradeItemCreateDTO, TradeItemVO, TradeSearchParams } from '../types/trade'
import { mockCreateItem, mockGetItem, mockSearchItems } from './_mock'

async function withMock<T>(real: () => Promise<T>, mock: () => T): Promise<T> {
  if (!import.meta.env.DEV) return real()
  try {
    return await real()
  } catch (err) {
    if (err instanceof BizError && err.httpStatus !== undefined && err.httpStatus < 500) throw err
    // eslint-disable-next-line no-console
    console.warn('[mock] trade API 后端未响应，使用 mock')
    return mock()
  }
}

export const searchItems = (params: TradeSearchParams) =>
  withMock<PageResponse<TradeItemVO>>(
    () => apiGet('/api/search/items', params),
    () => mockSearchItems(params),
  )

export const getItem = (itemId: number | string) =>
  withMock<TradeItemVO>(
    () => apiGet(`/api/trade/items/${itemId}`),
    () => mockGetItem(String(itemId)),
  )

// 后端 POST /api/trade/items 返回完整 TradeItemVO（schema_audit A-3/A-4 修复后从 multipart 改 JSON）
export const createItem = (dto: TradeItemCreateDTO) =>
  withMock<TradeItemVO>(
    () => apiPost('/api/trade/items', dto),
    () => mockCreateItem(dto),
  )
