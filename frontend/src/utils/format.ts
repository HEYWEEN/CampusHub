/**
 * 通用格式化工具
 */

export function formatRelativeTime(iso: string): string {
  const t = new Date(iso).getTime()
  const now = Date.now()
  const diff = t - now           // 正：未来；负：过去
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
  return { text: formatRelativeTime(iso), urgent, expired }
}

export function formatLocalDateTime(date: Date): string {
  // 返回 datetime-local 输入框需要的格式 YYYY-MM-DDTHH:mm
  const pad = (n: number) => String(n).padStart(2, '0')
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}`
  )
}
