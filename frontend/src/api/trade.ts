import { apiGet, apiPost } from './client'
import { BizError, type PageResponse } from '../types/api'
import type { TradeItemCreateDTO, TradeItemVO, TradeSearchParams } from '../types/trade'
import { mockCreateItem, mockGetItem, mockSearchItems } from './_mock'

async function withMock<T>(real: () => Promise<T>, mock: () => T): Promise<T> {
  if (!import.meta.env.DEV) return real()
  try {
    return await real()
  } catch (err) {
    if (err instanceof BizError && err.code !== 0 && err.code !== 404 && err.code < 500) throw err
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

export const getItem = (itemId: string) =>
  withMock<TradeItemVO>(
    () => apiGet(`/api/trade/items/${itemId}`),
    () => mockGetItem(itemId),
  )

export const createItem = (dto: TradeItemCreateDTO) =>
  withMock<{ itemId: string }>(
    () => apiPost('/api/trade/items', dto),
    () => ({ itemId: mockCreateItem(dto) }),
  )
