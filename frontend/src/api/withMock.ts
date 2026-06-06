import { BizError } from '../types/api'

/**
 * mock 回退「显式开关」，默认关闭（仅 dev 且 VITE_USE_MOCK=1 时启用）。
 *
 * 为何默认关：start.sh 并行拉起前后端，后端启动需 10~30s。若开着 mock，
 * 用户在后端就绪前点进来 → 首批请求失败 → 回退假数据（张大锤）并被 React Query
 * 当成功缓存 → 必须手动刷新才拿真数据。默认关后，后端没起来就抛真实错误，
 * 交给 React Query 重试，后端一就绪自动恢复，无需刷新。
 *
 * 想纯前端、无后端联调时：在 frontend/ 下设 VITE_USE_MOCK=1（如 .env.local）。
 */
const USE_MOCK = import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === '1'

/**
 * @param tag 模块名，仅用于 dev 控制台提示（如 'credit' / 'trade'）。
 */
export async function withMock<T>(
  real: () => Promise<T>,
  mock: () => T,
  tag = '',
): Promise<T> {
  if (!USE_MOCK) return real()
  try {
    return await real()
  } catch (err) {
    if (err instanceof BizError && err.httpStatus !== undefined && err.httpStatus < 500) throw err
    // eslint-disable-next-line no-console
    console.warn(`[mock] ${tag ? tag + ' API ' : ''}后端未响应，使用 mock`)
    return mock()
  }
}
