import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getMe } from '../../api/user'
import { getMyCredit } from '../../api/credit'
import { useAuthStore } from '../../stores/auth'
import { useCreditStore } from '../../stores/credit'
import './User.css'

export default function MePage() {
  const navigate = useNavigate()
  const logout = useAuthStore((s) => s.logout)
  const hydrateCredit = useCreditStore((s) => s.hydrate)

  const { data: me, isLoading: loadingMe } = useQuery({
    queryKey: ['me'],
    queryFn: () => getMe(),
  })

  const { data: credit } = useQuery({
    queryKey: ['credit', 'me'],
    queryFn: () => getMyCredit(),
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
  const PRIVACY_LABELS: { key: keyof typeof me.privacy; label: string; desc: string }[] = [
    { key: 'hidePublishHist',   label: '隐藏我发布的任务', desc: '公开主页不显示发布历史列表' },
    { key: 'hideAcceptHist',    label: '隐藏我接的任务',   desc: '公开主页不显示接单记录' },
    { key: 'hideCourseReviews', label: '隐藏我的课程评价', desc: '我的课评在评价区匿名展示' },
    { key: 'imOpen',            label: '接收私信',         desc: '关闭后陌生人不能给我发私信' },
  ]

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">
          Hi, <span className="it">{me.nickname}</span>.
        </h1>
        <div className="page-sub">我的主页 · 个人 · 信用 · 隐私</div>
      </div>

      <div className="me-layout">
        {/* 左栏 */}
        <aside className="me-side">
          <div className="me-avatar">
            {me.avatarUrl ? <img src={me.avatarUrl} alt={me.nickname} /> : <span>{ch}</span>}
          </div>
          <h2 className="me-name">{me.nickname}</h2>
          {me.verifiedTag && <div className="me-tag">{me.verifiedTag}</div>}
          <div className="me-phone">{me.phoneMasked}</div>

          <div className="me-side-actions">
            <Link to="/app/me/edit" className="me-action-btn is-primary">
              编辑资料 / 隐私 →
            </Link>
            <Link to={`/u/${me.userId}`} className="me-action-btn is-ghost">
              查看公开主页
            </Link>
            <button
              type="button"
              className="me-action-btn is-danger"
              onClick={() => {
                logout()
                navigate('/login')
              }}
            >
              退出登录
            </button>
          </div>
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
              </div>
              <div className="credit-stat">
                <div className="credit-stat-num">{credit?.pointBalance ?? 0}</div>
                <div className="credit-stat-label">可用积分</div>
              </div>
              <div className="credit-stat">
                <div className="credit-stat-num is-frozen">{credit?.pointFrozen ?? 0}</div>
                <div className="credit-stat-label">冻结积分</div>
              </div>
              <div className="credit-stat">
                <div className="credit-stat-num">
                  {credit?.dailyAcceptLimit ?? me.dailyAcceptLimit}
                  <span className="credit-stat-unit">/3</span>
                </div>
                <div className="credit-stat-label">日接单上限</div>
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
                <span className="me-stat-num"><span className="it">5</span></span>
                <span className="me-stat-label">我发布的任务</span>
              </Link>
              <Link to="/app/tasks/my" className="me-stat-link">
                <span className="me-stat-num"><span className="it">12</span></span>
                <span className="me-stat-label">我接过的任务</span>
              </Link>
              <Link to="/app/credit" className="me-stat-link">
                <span className="me-stat-num"><span className="it">22</span></span>
                <span className="me-stat-label">收到的评价</span>
              </Link>
            </div>
          </section>

          {/* 隐私概览 */}
          <section className="me-card">
            <div className="me-card-head">
              <h3 className="me-card-title">
                隐私 <span className="it">设置</span>
              </h3>
              <Link to="/app/me/edit" className="me-card-link">编辑 →</Link>
            </div>
            <div className="privacy-list">
              {PRIVACY_LABELS.map((p) => {
                const on = !!me.privacy[p.key]
                return (
                  <div className="privacy-item" key={p.key}>
                    <div className="privacy-text">
                      <span className="privacy-name">{p.label}</span>
                      <span className="privacy-desc">{p.desc}</span>
                    </div>
                    <span className={`toggle${on ? ' is-on' : ''}`} aria-hidden />
                  </div>
                )
              })}
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}
