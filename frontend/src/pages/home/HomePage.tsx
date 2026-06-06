import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import AppHeader from '../../components/layout/AppHeader'
import AgentWidget from '../../components/agent/AgentWidget'
import TaskCard from '../../components/domain/TaskCard'
import { getRecommendedTasks } from '../../api/recommend'
import { listNotifications } from '../../api/notify'
import { useAuthStore } from '../../stores/auth'
import './HomePage.css'

/* ─────────── 公告条：登录态拉最新站内信，未登录/空回退安全提示 ─────────── */
const NOTICE_TAG: Record<string, string> = {
  TRADE_OFFER_NEW: '议价', TRADE_OFFER_COUNTERED: '议价', TRADE_OFFER_ACCEPTED: '成交',
  TRADE_OFFER_REJECTED: '议价', TRADE_OFFER_CANCELED: '议价', TRADE_ORDER_CANCELED: '订单',
  TASK_ACCEPTED: '接单', TASK_COMPLETED: '完成', TASK_CANCELED: '取消', TASK_EXPIRED: '过期',
  VERIFY_RESULT: '认证', CREDIT_APPEAL_RESULT: '申诉', TEAM_APPLY_RESULT: '组队',
  REPORT_RESULT: '举报', REPORT_ACTION: '处罚', ACCOUNT_BAN: '账号', ROLE_CHANGE: '权限',
}

function fmtMD(iso: string): string {
  const d = new Date(iso)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function HomeNotice() {
  const isLoggedIn = !!useAuthStore((s) => s.accessToken)
  const { data } = useQuery({
    queryKey: ['notify', 'home-latest'],
    queryFn: () => listNotifications('all'),
    enabled: isLoggedIn,
    staleTime: 60_000,
  })
  const latest = isLoggedIn ? data?.[0] : undefined

  return (
    <section className="home-notice rise" data-rise data-delay="200">
      <span className="notice-icon" aria-hidden>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 11v2a1 1 0 0 0 1 1h2l5 4V6L6 10H4a1 1 0 0 0-1 1z" />
          <path d="M16 8a5 5 0 0 1 0 8" />
          <path d="M19 5a8 8 0 0 1 0 14" />
        </svg>
      </span>
      <span className="notice-label">{latest ? '消息' : '公告'}</span>
      <span className="notice-sep" />
      <span className="notice-tag">【{latest ? (NOTICE_TAG[latest.type] ?? '通知') : '重要'}】</span>
      <span className="notice-text">{latest ? latest.title : '关于谨防校园诈骗的提醒'}</span>
      <span className="notice-date mono">{latest ? fmtMD(latest.createdAt) : '05-26'}</span>
      <Link to="/app/notify" className="notice-more">
        查看全部 <span aria-hidden>→</span>
      </Link>
    </section>
  )
}

/* ─────────── 为你推荐（仅登录态；空/未登录自隐藏） ─────────── */
function HomeRecommend() {
  const accessToken = useAuthStore((s) => s.accessToken)
  const isLoggedIn = !!accessToken
  const { data } = useQuery({
    queryKey: ['recommend', 'tasks'],
    queryFn: () => getRecommendedTasks(8),
    enabled: isLoggedIn,
    staleTime: 60_000,
  })

  if (!isLoggedIn || !data || data.length === 0) return null

  return (
    <section className="home-recommend">
      <div className="home-recommend-head">
        <h2 className="home-recommend-title">为你推荐</h2>
        <Link to="/app/tasks" className="home-recommend-more">
          查看全部 <span aria-hidden>→</span>
        </Link>
      </div>
      <div className="home-recommend-grid">
        {data.map((t) => (
          <TaskCard key={t.taskId} task={t} />
        ))}
      </div>
    </section>
  )
}

/* ─────────── 4 大功能 Tile ─────────── */
type TileIcon = 'run' | 'bag' | 'cap' | 'team'
const TILES: {
  to: string
  num: string
  cn: string
  desc: string
  illo: string
  icon: TileIcon
}[] = [
    {
      to: '/app/tasks',
      num: '01',
      cn: '跑腿',
      desc: '取快递、带饭、占座…\n让忙碌的你更轻松',
      illo: '/illustrations/free-time.png',
      icon: 'run',
    },
    {
      to: '/app/trade',
      num: '02',
      cn: '二手',
      desc: '同校面交，安全可靠\n闲置物品流转起来',
      illo: '/illustrations/coffee.png',
      icon: 'bag',
    },
    {
      to: '/app/edu/tutor',
      num: '03',
      cn: '辅导',
      desc: '学长学姐在线帮忙\n难题不再难',
      illo: '/illustrations/focused.png',
      icon: 'cap',
    },
    {
      to: '/app/team',
      num: '04',
      cn: '组队',
      desc: '比赛 / 课设 / 毕设\n一起组队搞定',
      illo: '/illustrations/catching-up.png',
      icon: 'team',
    },
  ]

/* ─────────── Tile 顶部小图标（手写 SVG，单色描线） ─────────── */
function TileIconSvg({ kind }: { kind: TileIcon }) {
  const common = {
    width: 22,
    height: 22,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
  }
  switch (kind) {
    case 'run':
      return (
        <svg {...common}>
          {/* 奔跑小人 — 头部实心，躯干 + 前后摆动的四肢 */}
          <circle cx="16" cy="4.5" r="1.8" fill="currentColor" stroke="none" />
          <path d="M11 21l2.5-5 -2-2.5 3-4 2.5 2.5 3-.5" />
          <path d="M10 13l-3 .5" />
          <path d="M14.5 9.5l-3 1.5" />
        </svg>
      )
    case 'bag':
      return (
        <svg {...common}>
          <path d="M6 8h12l-1.2 11.2a1.5 1.5 0 0 1-1.5 1.3H8.7a1.5 1.5 0 0 1-1.5-1.3L6 8z" />
          <path d="M9 8V6a3 3 0 0 1 6 0v2" />
        </svg>
      )
    case 'cap':
      return (
        <svg {...common}>
          <path d="M2 9l10-4 10 4-10 4L2 9z" />
          <path d="M6 11v4c0 1.4 2.7 3 6 3s6-1.6 6-3v-4" />
          <path d="M22 9v5" />
        </svg>
      )
    case 'team':
      return (
        <svg {...common}>
          <circle cx="8" cy="9" r="2.6" />
          <circle cx="16" cy="9" r="2.6" />
          <path d="M3 19c.7-2.8 2.7-4.4 5-4.4s4.3 1.6 5 4.4" />
          <path d="M13 19c.7-2.8 2.7-4.4 5-4.4s4.3 1.6 5 4.4" />
        </svg>
      )
  }
}

/* ─────────── 底部统计 ─────────── */
// TODO: F-STATS-01 - 接 /api/stats 拉真实数据，目前为静态展示
const STATS = [
  { num: '1286+', label: '活跃用户', icon: 'users' as const },
  { num: '3421+', label: '累计完成任务', icon: 'check' as const },
  { num: '98%', label: '好评率', icon: 'heart' as const },
  { num: '100%', label: '实名认证', icon: 'shield' as const },
]

function StatIcon({ kind }: { kind: 'users' | 'check' | 'heart' | 'shield' }) {
  const c = {
    width: 20, height: 20, viewBox: '0 0 24 24', fill: 'none',
    stroke: 'currentColor', strokeWidth: 1.7,
    strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const,
  }
  switch (kind) {
    case 'users':
      return (
        <svg {...c}>
          <circle cx="9" cy="9" r="3" />
          <path d="M3 19c.6-3 2.9-4.8 6-4.8s5.4 1.8 6 4.8" />
          <circle cx="17" cy="8" r="2.5" />
          <path d="M14.7 14.5c1-.4 2.1-.6 3.3-.6 2.4 0 3.8 1 4.5 2.6" />
        </svg>
      )
    case 'check':
      return (
        <svg {...c}>
          <rect x="5" y="4" width="14" height="17" rx="2" />
          <path d="M9 11l2 2 4-4" />
        </svg>
      )
    case 'heart':
      return (
        <svg {...c}>
          <path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z" />
        </svg>
      )
    case 'shield':
      return (
        <svg {...c}>
          <path d="M12 3l8 3v5c0 5-3.4 8.6-8 10-4.6-1.4-8-5-8-10V6l8-3z" />
          <path d="M9 12l2 2 4-4" />
        </svg>
      )
  }
}

export default function HomePage() {
  useEffect(() => {
    /* ── split text（仅 H1 楷书） ── */
    let chIdx = 0
    const splitNode = (node: Node) => {
      if (node.nodeType === Node.TEXT_NODE) {
        const frag = document.createDocumentFragment()
        const text = node.textContent ?? ''
          ;[...text].forEach((c) => {
            const s = document.createElement('span')
            s.className = 'ch'
            s.textContent = c === ' ' ? ' ' : c
            s.style.transitionDelay = `${chIdx++ * 28}ms`
            frag.appendChild(s)
          })
        node.parentNode?.replaceChild(frag, node)
      } else if (node.nodeType === Node.ELEMENT_NODE) {
        ;[...node.childNodes].forEach(splitNode)
      }
    }
    document.querySelectorAll<HTMLElement>('[data-split]').forEach((el) => {
      if (el.dataset.splitDone === '1') return
      chIdx = 0
        ;[...el.childNodes].forEach(splitNode)
      el.dataset.splitDone = '1'
    })

    /* ── scroll reveal ── */
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (!e.isIntersecting) return
          const el = e.target as HTMLElement
          const delay = parseInt(el.dataset.delay || '0', 10)
          if (el.matches('[data-split]')) {
            setTimeout(() => {
              el.querySelectorAll('.ch').forEach((ch) => ch.classList.add('in'))
            }, delay)
          } else {
            setTimeout(() => el.classList.add('in'), delay)
          }
          io.unobserve(el)
        })
      },
      { threshold: 0.12, rootMargin: '0px 0px -8% 0px' },
    )
    document
      .querySelectorAll<HTMLElement>('[data-rise],[data-split]')
      .forEach((el) => io.observe(el))

    /* ── cursor follower ── */
    const cur = document.getElementById('cursor')
    let mx = 0, my = 0, cx = 0, cy = 0, raf = 0
    const onMove = (e: MouseEvent) => { mx = e.clientX; my = e.clientY }
    window.addEventListener('mousemove', onMove)
    const frame = () => {
      cx += (mx - cx) * 0.18
      cy += (my - cy) * 0.18
      if (cur) cur.style.transform = `translate(${cx}px,${cy}px) translate(-50%,-50%)`
      raf = requestAnimationFrame(frame)
    }
    frame()
    const hovers = document.querySelectorAll<HTMLElement>('a, button, .tile')
    const onEnter = () => cur?.classList.add('lg')
    const onLeave = () => cur?.classList.remove('lg')
    hovers.forEach((el) => {
      el.addEventListener('mouseenter', onEnter)
      el.addEventListener('mouseleave', onLeave)
    })

    return () => {
      window.removeEventListener('mousemove', onMove)
      cancelAnimationFrame(raf)
      io.disconnect()
      hovers.forEach((el) => {
        el.removeEventListener('mouseenter', onEnter)
        el.removeEventListener('mouseleave', onLeave)
      })
    }
  }, [])

  return (
    <>
      <div className="cursor" id="cursor" />
      <AppHeader />

      <main className="home-shell">
        {/* ─────────── HERO（双列：左文 + 右钟楼） ─────────── */}
        <section className="home-hero">
          <div className="hero-left">
            <div className="eyebrow rise" data-rise>
              <span className="pulse" />
              <span>南大人 · 帮南大人 · 2026</span>
              <span className="ln" />
            </div>

            <h1 className="home-title">
              <span className="row split" data-split>南京大学</span>
              <span className="row split" data-split data-delay="120">
                校园<span className="it">互助</span>平台
              </span>
            </h1>

            <p className="home-sub rise" data-rise data-delay="320">
              跑腿、二手、辅导、组队，全部在这里完成。<br />
              互帮互助，让校园生活更简单。
            </p>


          </div>

          <div className="hero-right rise" data-rise data-delay="220">
            <img
              className="hero-illo"
              src="/illustrations/beidalou.png"
              alt="南京大学北大楼"
              loading="eager"
              decoding="async"
            />
          </div>
        </section>

        {/* ─────────── 4 大功能 Tile ─────────── */}
        <section className="home-tiles">
          {TILES.map((t, i) => (
            <Link
              key={t.to}
              to={t.to}
              className="tile rise"
              data-rise
              data-delay={String(120 + i * 80)}
            >
              <div className="tile-head">
                <span className="tile-icon" aria-hidden>
                  <TileIconSvg kind={t.icon} />
                </span>
                <span className="tile-num">{t.num}</span>
              </div>

              <h3 className="tile-title">{t.cn}</h3>
              <p className="tile-desc">
                {t.desc.split('\n').map((line, idx) => (
                  <span key={idx} className="tile-desc-line">{line}</span>
                ))}
              </p>

              <div className="tile-foot">
                <span className="tile-cta">
                  进入 <span aria-hidden>→</span>
                </span>
                <img className="tile-illo" src={t.illo} alt="" loading="lazy" />
              </div>
            </Link>
          ))}
        </section>

        {/* ─────────── 为你推荐（P2 智能匹配，登录可见） ─────────── */}
        <HomeRecommend />

        {/* ─────────── 公告条（最新站内信 / 回退安全提示） ─────────── */}
        <HomeNotice />

        {/* ─────────── 底部统计带 ─────────── */}
        <section className="home-stats rise" data-rise data-delay="240">
          <div className="stats-grid">
            {STATS.map((s) => (
              <div key={s.label} className="stat-item">
                <span className="stat-icon" aria-hidden>
                  <StatIcon kind={s.icon} />
                </span>
                <div className="stat-text">
                  <div className="stat-num">{s.num}</div>
                  <div className="stat-label">{s.label}</div>
                </div>
              </div>
            ))}
          </div>
          <img
            className="stats-illo"
            src="/illustrations/nju-gate.png"
            alt=""
            loading="lazy"
            aria-hidden
          />
        </section>

        {/* ─────────── FOOTER ─────────── */}
        <footer className="home-foot">
          <div className="home-foot-left">
            <span className="dot-small" />
            <span>CampusHub · v 1.0</span>
          </div>
          <div className="home-foot-right mono">
            南京大学 · 软件工程与计算II大作业 · 2026 · 開發者的力量团队
          </div>
        </footer>
      </main>
      <AgentWidget />
    </>
  )
}
