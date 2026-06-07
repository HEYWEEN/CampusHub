import { apiGet, apiPost, apiDelete } from './client'
import { type PageResponse } from '../types/api'
import type { CreditAppealVO, CreditMeVO, CreditRecord, ReceivedReviewVO, ReviewResultVO } from '../types/credit'
import {
  mockGetCredit,
  mockListCreditRecords,
  mockListMyAppeals,
  mockReceivedReviews,
  mockSubmitAppeal,
} from './_mock'
import { withMock } from './withMock'

export const getMyCredit = () =>
  withMock<CreditMeVO>(
    () => apiGet('/api/credits/me'),
    () => mockGetCredit(),
    'credit',
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

// ───── 任务评价（CRD-02） ─────

/** 提交对任务对方的评分。reviewer 取自登录态;同一任务重复提交后端返回 409。 */
export const submitReview = (taskId: number, revieweeId: number, rating: number, comment: string) =>
  withMock<ReviewResultVO>(
    () => apiPost('/api/credit/reviews', { taskId, revieweeId, rating, comment }),
    () => ({ reviewId: Date.now(), bothReviewed: false }),
    'credit',
  )

// ───── 信用申诉（F-CREDIT-05~07） ─────

export const listReceivedReviews = () =>
  withMock<ReceivedReviewVO[]>(
    () => apiGet('/api/credit/reviews/received'),
    () => mockReceivedReviews(),
    'credit',
  )

export const submitAppeal = (reviewId: number, reason: string, evidenceUrls: string[]) =>
  withMock<CreditAppealVO>(
    () => apiPost('/api/credit/appeals', { reviewId, reason, evidenceUrls }),
    () => mockSubmitAppeal(reviewId, reason),
    'credit',
  )

export const listMyAppeals = () =>
  withMock<CreditAppealVO[]>(
    () => apiGet('/api/credit/appeals/me'),
    () => mockListMyAppeals(),
    'credit',
  )

/** 从「我收到的评价」删除一条已撤销的差评（软删，仅本人视图）。 */
export const hideReceivedReview = (reviewId: number) =>
  apiDelete<void>(`/api/credit/reviews/received/${reviewId}`)

/** 从「我的申诉」删除一条已处理的申诉记录（软删，仅本人视图）。 */
export const hideAppeal = (appealId: number) =>
  apiDelete<void>(`/api/credit/appeals/${appealId}`)
