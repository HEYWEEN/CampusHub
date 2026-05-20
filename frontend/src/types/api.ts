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

export class BizError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'BizError'
  }
}
