/**
 * 与后端 common/response/ApiResponse 对齐的核心类型
 */

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
  userId: string
  verifyStatus: 'guest' | 'pending' | 'approved' | 'rejected'
}

/**
 * 业务/HTTP 错误统一异常。
 *
 * - code：后端业务码（如 6001 = 未认证）；网络/无响应时 = 0
 * - httpStatus：HTTP 状态码（4xx / 5xx）；网络/无响应时 = undefined
 *
 * withMock fallback 判断必须用 httpStatus（5xx 或 undefined → fallback），
 * 不能用 code，因为业务码常常是 4 位数（如 6001），数值上大于 500 会被误判。
 */
export class BizError extends Error {
  code: number
  httpStatus?: number
  constructor(code: number, message: string, httpStatus?: number) {
    super(message)
    this.code = code
    this.httpStatus = httpStatus
    this.name = 'BizError'
  }
}
