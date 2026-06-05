/**
 * 通用格式化工具
 */

// 容忍服务端/客户端时钟偏移 + 网络往返：刚生成的时间戳可能比本地 now 略快。
// 这段窗口内一律归一为「刚刚」，避免过去事件显示成「即将」。
const CLOCK_SKEW_MS = 30_000

/**
 * 过去时间戳（createdAt / lastMsgAt 等）→「刚刚 / X 分钟前 / …」。
 * 严格「过去」语义：小幅未来（时钟偏移）也归一为「刚刚」，绝不输出「即将」。
 * 需要「未来」文案（倒计时）的场景请用 {@link formatDeadline}。
 */
export function formatRelativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()   // 正：过去；负：未来（视为时钟偏移）
  if (diff < CLOCK_SKEW_MS) return '刚刚'
  const min = Math.round(diff / 60_000)
  if (min < 60) return `${min} 分钟前`
  const hr = Math.round(min / 60)
  if (hr < 24) return `${hr} 小时前`
  const day = Math.round(hr / 24)
  if (day < 30) return `${day} 天前`
  return new Date(iso).toLocaleDateString('zh-CN')
}

/** 双向相对时间：deadline 既可能在未来（倒计时）也可能已过期。 */
function formatSignedRelative(iso: string): string {
  const diff = new Date(iso).getTime() - Date.now()   // 正：未来；负：过去
  const abs = Math.abs(diff)
  const min = Math.round(abs / 60_000)

  if (min < 1) return diff >= 0 ? '即将' : '刚刚'
  if (min < 60) return diff >= 0 ? `${min} 分钟后` : `${min} 分钟前`
  const hr = Math.round(min / 60)
  if (hr < 24) return diff >= 0 ? `${hr} 小时后` : `${hr} 小时前`
  const day = Math.round(hr / 24)
  if (day < 30) return diff >= 0 ? `${day} 天后` : `${day} 天前`
  return new Date(iso).toLocaleDateString('zh-CN')
}

export function formatDeadline(iso: string): { text: string; urgent: boolean; expired: boolean } {
  const t = new Date(iso).getTime()
  const diff = t - Date.now()
  const expired = diff < 0
  const urgent = !expired && diff < 60 * 60_000 // 1 小时内 = urgent
  return { text: formatSignedRelative(iso), urgent, expired }
}

export function formatLocalDateTime(date: Date): string {
  // 返回 datetime-local 输入框需要的格式 YYYY-MM-DDTHH:mm
  const pad = (n: number) => String(n).padStart(2, '0')
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}`
  )
}
