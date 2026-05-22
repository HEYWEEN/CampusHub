import { useEffect } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '../../stores/auth'
import { useNotifyStore } from '../../stores/notify'
import { getUnreadCount } from '../../api/notify'

const NAV = [
  { to: '/app/tasks',     label: '任务' },
  { to: '/app/trade',     label: '二手' },
  { to: '/app/edu/tutor', label: '辅导' },
  { to: '/app/team',      label: '组队' },
  { to: '/app/im',        label: '消息' },
  { to: '/app/me',        label: '我的' },
] as const

/**
 * 全站通用 Header
 * — 未登录：logo + "登录使用" pill
 * — 已登录：logo + 6 nav link + 通知 badge + 退出
 * — sticky on scroll（自治，不依赖外层 layout）
 */
export default function AppHeader() {
  const navigate = useNavigate()
  const accessToken = useAuthStore((s) => s.accessToken)
  const isLoggedIn = !!accessToken
  const logout = useAuthStore((s) => s.logout)
  const unread = useNotifyStore((s) => s.unreadCount)
  const setUnread = useNotifyStore((s) => s.setUnread)

  // sticky scroll
  useEffect(() => {
    const hdr = document.getElementById('app-hdr')
    const onScroll = () => hdr?.classList.toggle('scrolled', window.scrollY > 16)
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  // 未读数量 — 只在登录后拉
  const { data } = useQuery({
    queryKey: ['notify-unread'],
    queryFn: () => getUnreadCount(),
    refetchInterval: 30_000,
    enabled: isLoggedIn,
  })
  useEffect(() => {
    if (data) setUnread(data.count)
  }, [data, setUnread])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header id="app-hdr" className="app-header">
      <Link to="/" className="logo">
        <span className="dot" />
        <span>CampusHub</span>
      </Link>

      {isLoggedIn ? (
        <>
          <nav className="app-nav">
            {NAV.map((n) => (
              <NavLink
                key={n.to}
                to={n.to}
                className={({ isActive }) => `app-nav-link${isActive ? ' is-active' : ''}`}
              >
                {n.label}
              </NavLink>
            ))}
          </nav>

          <div className="app-header-right">
            <NavLink to="/app/notify" className="app-icon-btn" aria-label="通知">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
                <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
              </svg>
              {unread > 0 && <span className="app-badge">{unread > 99 ? '99+' : unread}</span>}
            </NavLink>
            <button onClick={handleLogout} className="app-pill" type="button">
              退出 <span className="arr">→</span>
            </button>
          </div>
        </>
      ) : (
        <Link to="/login" className="app-pill">
          登录使用 <span className="arr">→</span>
        </Link>
      )}
    </header>
  )
}
