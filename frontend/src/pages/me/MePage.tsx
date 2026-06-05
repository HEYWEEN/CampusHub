import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getMe, getMyStats } from '../../api/user'
import { getMyCredit } from '../../api/credit'
import { logout as logoutApi } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'
import { useCreditStore } from '../../stores/credit'
import './User.css'

/** score → 档位文案（信用卡 sub-status） */
function creditTier(score: number): string {
  if (score >= 80) return '优秀 ⭐'
  if (score >= 60) return '良好'
  return '需改进'
}

/** joinedAt(ISO) → 加入天数 + 中文日期 */
function joinInfo(iso?: string): { days: number; date: string } | null {
  if (!iso) return null
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return null
  const days = Math.max(0, Math.floor((Date.now() - d.getTime()) / 86_400_000))
  const date = `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
  return { days, date }
}

export default function MePage() {
  const navigate = useNavigate()
  const clearAuth = useAuthStore((s) => s.logout)
  const refreshToken = useAuthStore((s) => s.refreshToken)
  const hydrateCredit = useCreditStore((s) => s.hydrate)

  const handleLogout = async () => {
    // 让后端把 refresh token 入黑名单（schema_audit A-13）；失败也照样清本地态
    try { await logoutApi(refreshToken) } catch { /* ignore */ }
    clearAuth()
    navigate('/login')
  }

  const { data: me, isLoading: loadingMe } = useQuery({
    queryKey: ['me'],
    queryFn: () => getMe(),
  })

  const { data: credit } = useQuery({
    queryKey: ['credit', 'me'],
    queryFn: () => getMyCredit(),
  })

  const { data: stats } = useQuery({
    queryKey: ['me', 'stats'],
    queryFn: () => getMyStats(),
  })

  // 把信用数据同步到 Zustand store，header / 其他页面也能看到
  useEffect(() => {
    if (credit) hydrateCredit(credit)
  }, [credit, hydrateCredit])

  if (loadingMe || !me) {
    return (
      <div className="wrap">
        <div className="task-loading">加载中…</div>
      </div>
    )
  }

  const ch = (me.nickname?.[0] ?? '我').toUpperCase()
  const score = credit?.creditScore ?? 80
  const frozen = credit?.pointFrozen ?? 0
  const verified = me.verifyStatus === 'approved'
  const join = joinInfo(me.joinedAt)

  return (
    <div className="wrap">
      {/* ─── Hero ─── */}
      <div className="me-hero">
        <div className="me-hero-text">
          <h1 className="me-greeting">
            Hi, <span className="it">{me.nickname}</span>.
          </h1>
          <p className="me-hero-sub">欢迎回来，继续互助让校园更美好 ✨</p>
        </div>
        <img className="me-hero-illo" src="/illustrations/nju-gate.png" alt="" aria-hidden />
      </div>

      <div className="me-layout">
        {/* 左栏 */}
        <aside className="me-side">
          <div className="me-avatar">
            {me.avatarUrl ? <img src={me.avatarUrl} alt={me.nickname} /> : <span>{ch}</span>}
          </div>
          <h2 className="me-name">{me.nickname}</h2>
          {verified && (
            <div className="me-badge">
              <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden>
                <path d="M6.5 10.5 4 8l1-1 1.5 1.5L11 4l1 1-5.5 5.5Z" fill="currentColor" />
              </svg>
              校园已认证
            </div>
          )}
          <div className="me-phone">ID: {me.phoneMasked}</div>

          <div className="me-side-actions">
            <Link to="/app/me/edit" className="me-action-btn is-primary">
              编辑资料 / 隐私设置 →
            </Link>
            <Link to={`/u/${me.userId}`} className="me-action-btn is-ghost">
              查看公开主页
            </Link>
            <button
              type="button"
              className="me-action-btn is-danger"
              onClick={handleLogout}
            >
              退出登录
            </button>
          </div>

          {/* 未认证 → 学生证认证入口；已认证 → 加入天数 */}
          {!verified ? (
            <Link to="/verify" className="me-aux-card is-verify">
              <span className="me-aux-label">学生证认证 →</span>
              <span className="me-aux-sub">认证后解锁完整功能</span>
            </Link>
          ) : join ? (
            <div className="me-aux-card">
              <span className="me-aux-label">加入 CampusHub</span>
              <span className="me-aux-num">{join.days} <em>天</em></span>
              <span className="me-aux-sub">{join.date}加入</span>
            </div>
          ) : null}
        </aside>

        {/* 右栏 */}
        <div className="me-main">
          {/* 信用 & 积分 */}
          <section className="me-card">
            <div className="me-card-head">
              <h3 className="me-card-title">
                信用 · <span className="it">积分</span>
              </h3>
              <Link to="/app/credit" className="me-card-link">流水详情 →</Link>
            </div>
            <div className="credit-grid">
              <div className="credit-stat">
                <div className="credit-stat-num is-accent">{score}</div>
                <div className="credit-stat-label">信用分</div>
                <div className="credit-stat-sub">{creditTier(score)}</div>
              </div>
              <div className="credit-stat">
                <div className="credit-stat-num">{credit?.pointBalance ?? 0}</div>
                <div className="credit-stat-label">可用积分</div>
                <div className="credit-stat-sub">可提现</div>
              </div>
              <div className="credit-stat">
                <div className="credit-stat-num is-frozen">{frozen}</div>
                <div className="credit-stat-label">冻结积分</div>
                <div className="credit-stat-sub">{frozen > 0 ? `${frozen} 笔占用` : '无冻结'}</div>
              </div>
              <div className="credit-stat">
                <div className="credit-stat-num">
                  {credit?.dailyAcceptLimit ?? me.dailyAcceptLimit}
                  <span className="credit-stat-unit">/3</span>
                </div>
                <div className="credit-stat-label">日接单上限</div>
                <div className="credit-stat-sub">今日可接单</div>
              </div>
            </div>
            <div className="credit-bar">
              <div className="credit-bar-track">
                <div className="credit-bar-fill" style={{ width: `${Math.min(100, score)}%` }} />
              </div>
              <div className="credit-bar-meta">
                <span>0 · 禁止</span>
                <span>60 · 可发可接</span>
                <span>100 · 优秀</span>
              </div>
            </div>
            <div className="credit-hint">
              <span aria-hidden>ⓘ</span> 保持良好的完成率和评价，可以提升信用分哦
            </div>
          </section>

          {/* 任务统计 */}
          <section className="me-card">
            <div className="me-card-head">
              <h3 className="me-card-title">
                我的<span className="it">任务</span>
              </h3>
              <Link to="/app/tasks/my" className="me-card-link">全部 →</Link>
            </div>
            <div className="me-stats-row">
              <Link to="/app/tasks/my" className="me-stat-link">
                <span className="me-stat-num">{stats?.publishedCount ?? '—'}</span>
                <span className="me-stat-label">我发布的任务</span>
                <span className="me-stat-sub">进行中 {stats?.publishedInProgress ?? 0}</span>
              </Link>
              <Link to="/app/tasks/my" className="me-stat-link">
                <span className="me-stat-num">{stats?.acceptedCount ?? '—'}</span>
                <span className="me-stat-label">我接过的任务</span>
                <span className="me-stat-sub">进行中 {stats?.acceptedInProgress ?? 0}</span>
              </Link>
              <Link to="/app/credit" className="me-stat-link">
                <span className="me-stat-num">{stats?.reviewsCount ?? '—'}</span>
                <span className="me-stat-label">收到的评价</span>
                <span className="me-stat-sub">
                  {stats?.goodRate != null ? `好评率 ${stats.goodRate}%` : '暂无评价'}
                </span>
              </Link>
            </div>
          </section>

          {/* 二手交易 */}
          <section className="me-card">
            <div className="me-card-head">
              <h3 className="me-card-title">
                我的<span className="it">交易</span>
              </h3>
              <Link to="/app/trade/mine" className="me-card-link">全部 →</Link>
            </div>
            <div className="me-stats-row">
              <Link to="/app/trade/mine" className="me-stat-link">
                <span className="me-stat-label">砍价 / 订单</span>
                <span className="me-stat-sub">出价、还价、确认收货</span>
              </Link>
              <Link to="/app/trade" className="me-stat-link">
                <span className="me-stat-label">去二手大厅</span>
                <span className="me-stat-sub">逛逛在售好物</span>
              </Link>
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}
