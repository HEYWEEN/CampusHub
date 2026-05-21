import { apiGet } from './client'
import { BizError } from '../types/api'
import type { CreditMeVO, CreditRecord } from '../types/credit'
import { mockGetCredit, mockListCreditRecords } from './_mock'

async function withMock<T>(real: () => Promise<T>, mock: () => T): Promise<T> {
  if (!import.meta.env.DEV) return real()
  try {
    return await real()
  } catch (err) {
    if (err instanceof BizError && err.code !== 0 && err.code !== 404 && err.code < 500) throw err
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

export const listMyRecords = () =>
  withMock<CreditRecord[]>(
    () => apiGet('/api/credits/me/records'),
    () => mockListCreditRecords(),
  )
