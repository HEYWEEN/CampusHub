import { apiGet } from './client'
import { BizError, type PageResponse } from '../types/api'
import type { CreditMeVO, CreditRecord } from '../types/credit'
import { mockGetCredit, mockListCreditRecords } from './_mock'

async function withMock<T>(real: () => Promise<T>, mock: () => T): Promise<T> {
  if (!import.meta.env.DEV) return real()
  try {
    return await real()
  } catch (err) {
    if (err instanceof BizError && err.httpStatus !== undefined && err.httpStatus < 500) throw err
    // eslint-disable-next-line no-console
    console.warn('[mock] credit API 后端未响应，使用 mock')
    return mock()
  }
}

export const getMyCredit = () =>
  withMock<CreditMeVO>(
    () => apiGet('/api/credits/me'),
    () => mockGetCredit(),
  )

/**
 * 列表接口走分页（对齐后端 GET /api/credits/me/records → PageResponse<CreditRecordVO>）。
 * 调用方需要数组就解构 `.items`。
 * （schema_audit P0a-6 修复：原本前端期望数组导致 records.map 崩溃）
 */
export const listMyRecords = (page = 1, size = 20) =>
  withMock<PageResponse<CreditRecord>>(
    () => apiGet(`/api/credits/me/records?page=${page}&size=${size}`),
    () => {
      const items = mockListCreditRecords()
      return { items, total: items.length, page, size }
    },
  )
