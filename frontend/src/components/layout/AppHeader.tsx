import { useEffect, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '../../stores/auth'
import { useNotifyStore } from '../../stores/notify'
import { getUnreadCount } from '../../api/notify'
import { getUnread as getImUnread } from '../../api/im'
import { getMe } from '../../api/user'
import HelpModal from './HelpModal'

const NAV = [
  { to: '/',              label: '首页', end: true },
  { to: '/app/tasks',     label: '任务' },
  { to: '/app/trade',     label: '二手' },
  { to: '/app/edu/tutor', label: '辅导' },
  { to: '/app/team',      label: '组队' },
  { to: '/app/im',        label: '消息' },
] as const

/**
 * 全站通用 Header
 * — 未登录：logo + 「登录使用」pill
 * — 已登录：logo + 6 nav link + 通知 badge + 头像菜单（跳 /app/me）
 */
export default function AppHeader() {
  const navigate = useNavigate()
  const accessToken = useAuthStore((s) => s.accessToken)
  const isLoggedIn = !!accessToken
  const [helpOpen, setHelpOpen] = useState(false)
  const unread = useNotifyStore((s) => s.unreadCount)
  const setUnread = useNotifyStore((s) => s.setUnread)

  useEffect(() => {
    const hdr = document.getElementById('app-hdr')
    const onScroll = () => hdr?.classList.toggle('scrolled', window.scrollY > 16)
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const { data } = useQuery({
    queryKey: ['notify-unread'],
    queryFn: () => getUnreadCount(),
    refetchInterval: 30_000,
    enabled: isLoggedIn,
  })
  useEffect(() => {
    if (data) setUnread(data.count)
  }, [data, setUnread])

  const { data: imUnreadData } = useQuery({
    queryKey: ['im-unread'],
    queryFn: () => getImUnread(),
    refetchInterval: 15_000,
    enabled: isLoggedIn,
  })
  const imUnread = imUnreadData?.count ?? 0

  // 拉本人 me 用于 header 右上角昵称 + 头像（与 MePage 复用同一 queryKey 的缓存）
  const { data: me } = useQuery({
    queryKey: ['me'],
    queryFn: () => getMe(),
    enabled: isLoggedIn,
    staleTime: 60_000,
  })
  const displayName = me?.nickname || '同学'
  const avatarUrl = me?.avatarUrl || null
  const fallbackChar = (displayName?.[0] ?? '?').toUpperCase()

  return (
    <header id="app-hdr" className="app-header">
      <Link to="/" className="logo">
        <img className="logo-mark" src="/logo.png" alt="CampusHub" />
        <span>CampusHub</span>
      </Link>

      {isLoggedIn ? (
        <>
          <nav className="app-nav">
            {NAV.map((n) => (
              <NavLink
                key={n.to}
                to={n.to}
                end={'end' in n ? n.end : false}
                className={({ isActive }) => `app-nav-link${isActive ? ' is-active' : ''}`}
              >
                {n.label}
                {n.to === '/app/im' && imUnread > 0 && (
                  <span className="app-nav-badge">{imUnread > 99 ? '99+' : imUnread}</span>
                )}
              </NavLink>
            ))}
          </nav>

          <div className="app-header-right">
            <button type="button" className="app-icon-btn" aria-label="使用说明" onClick={() => setHelpOpen(true)}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="9" />
                <path d="M9.2 9a2.8 2.8 0 0 1 5.4 1c0 1.8-2.6 2.2-2.6 4" />
                <path d="M12 17.5h.01" />
              </svg>
            </button>
            {(me?.role === 'ADMIN' || me?.role === 'SUPER_ADMIN') && (
              <Link to="/admin" className="app-admin-link">管理后台</Link>
            )}
            <NavLink to="/app/notify" className="app-icon-btn" aria-label="通知">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
                <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
              </svg>
              {unread > 0 && <span className="app-badge">{unread > 99 ? '99+' : unread}</span>}
            </NavLink>

            <button
              type="button"
              className="app-user"
              aria-label="我的"
              onClick={() => navigate('/app/me')}
            >
              <span className="app-user-avatar" aria-hidden>
                {avatarUrl ? (
                  <img src={avatarUrl} alt="" />
                ) : (
                  <span className="app-user-avatar-fallback">{fallbackChar}</span>
                )}
              </span>
              <span className="app-user-name">{displayName}</span>
              <svg className="app-user-caret" width="10" height="10" viewBox="0 0 10 10" aria-hidden>
                <path d="M2 4l3 3 3-3" stroke="currentColor" strokeWidth="1.4" fill="none" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </div>
        </>
      ) : (
        <div className="app-header-right">
          <button type="button" className="app-icon-btn" aria-label="使用说明" onClick={() => setHelpOpen(true)}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="9" />
              <path d="M9.2 9a2.8 2.8 0 0 1 5.4 1c0 1.8-2.6 2.2-2.6 4" />
              <path d="M12 17.5h.01" />
            </svg>
          </button>
          <Link to="/login" className="app-pill">
            登录使用 <span className="arr">→</span>
          </Link>
        </div>
      )}

      <HelpModal open={helpOpen} onClose={() => setHelpOpen(false)} />
    </header>
  )
}
