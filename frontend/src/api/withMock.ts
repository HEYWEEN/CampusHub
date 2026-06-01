import { BizError } from '../types/api'

/**
 * dev 模式下，API 调用失败时自动回退到 mock；
 * 生产环境直接 throw，保证联调时能暴露真实错误。
 *
 * 业务错码（400/403/409 等，httpStatus < 500）始终保留真实错误，
 * 不用 mock 掩盖；仅网络错或 5xx 才 fallback。
 *
 * @param tag 模块名，仅用于 dev 控制台提示（如 'credit' / 'trade'）。
 */
export async function withMock<T>(
  real: () => Promise<T>,
  mock: () => T,
  tag = '',
): Promise<T> {
  if (!import.meta.env.DEV) return real()
  try {
    return await real()
  } catch (err) {
    if (err instanceof BizError && err.httpStatus !== undefined && err.httpStatus < 500) throw err
    // eslint-disable-next-line no-console
    console.warn(`[mock] ${tag ? tag + ' API ' : ''}后端未响应，使用 mock`)
    return mock()
  }
}
