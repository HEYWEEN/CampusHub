/**
 * 大厅页共用小部件：统计图标 + 安全保障卡片。
 * 任务 / 二手 / 辅导三个大厅共用，样式见 pages/tasks/Tasks.css。
 */

const SVG_BASE = {
  fill: 'none',
  stroke: 'currentColor',
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
}

export type StatIconName = 'clipboard' | 'clock' | 'check' | 'tag' | 'cart' | 'book'

/** Hero 统计数字上方的圆角图标徽章。 */
export function StatIcon({ name }: { name: StatIconName }) {
  const p = { ...SVG_BASE, width: 18, height: 18, viewBox: '0 0 24 24', strokeWidth: 1.7 }
  switch (name) {
    case 'clipboard':
      return <svg {...p}><rect x="6" y="4" width="12" height="17" rx="2" /><path d="M9 4a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 15 4" /><path d="M9 11h6M9 15h4" /></svg>
    case 'clock':
      return <svg {...p}><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></svg>
    case 'check':
      return <svg {...p}><circle cx="12" cy="12" r="8.5" /><path d="m8.5 12 2.5 2.5 4.5-5" /></svg>
    case 'tag':
      return <svg {...p}><path d="M3 12V5a2 2 0 0 1 2-2h7l9 9-9 9z" /><circle cx="8" cy="8" r="1.4" /></svg>
    case 'cart':
      return <svg {...p}><path d="M4 5h2l2 11h9l2-8H7" /><circle cx="9" cy="20" r="1.3" /><circle cx="17" cy="20" r="1.3" /></svg>
    case 'book':
      return <svg {...p}><path d="M5 4h9a2 2 0 0 1 2 2v14H7a2 2 0 0 1-2-2z" /><path d="M16 6h3v12a2 2 0 0 0-2 2H7" /></svg>
  }
}

function SafetyIcon({ name }: { name: 'badge' | 'shield' | 'chat' }) {
  const p = { ...SVG_BASE, width: 20, height: 20, viewBox: '0 0 24 24', strokeWidth: 1.6 }
  if (name === 'badge') return <svg {...p}><path d="M9 3h6l1 2h2a1 1 0 0 1 1 1v13a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6a1 1 0 0 1 1-1h2z" /><path d="m9 13 2 2 4-4" /></svg>
  if (name === 'shield') return <svg {...p}><path d="M12 3 5 6v5c0 4 3 7 7 9 4-2 7-5 7-9V6z" /><path d="m9 12 2 2 4-4" /></svg>
  return <svg {...p}><path d="M21 12a8 8 0 0 1-11.5 7.2L4 20l1-4.2A8 8 0 1 1 21 12z" /></svg>
}

const SAFETY = [
  { title: '实名认证', sub: '校内身份认证', icon: 'badge' },
  { title: '信用评价', sub: '真实可靠透明', icon: 'shield' },
  { title: '安全沟通', sub: '平台保障隐私', icon: 'chat' },
] as const

/** 右侧栏「安全保障」卡片（三大厅共用）。 */
export function SafetyCard() {
  return (
    <section className="aside-card safety-card">
      <div className="aside-head">
        <h2 className="aside-title">安全保障</h2>
      </div>
      <div className="safety-grid">
        {SAFETY.map((s) => (
          <div key={s.title} className="safety-item">
            <span className="safety-ic" aria-hidden><SafetyIcon name={s.icon} /></span>
            <div className="safety-title">{s.title}</div>
            <div className="safety-sub">{s.sub}</div>
          </div>
        ))}
      </div>
    </section>
  )
}
