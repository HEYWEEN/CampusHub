import { NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth'
import { useNotifyStore } from '../../stores/notify'

const NAV = [
  { to: '/app/tasks',     label: '任务' },
  { to: '/app/trade',     label: '二手' },
  { to: '/app/edu/tutor', label: '辅导' },
  { to: '/app/team',      label: '组队' },
  { to: '/app/im',        label: '消息' },
  { to: '/app/me',        label: '我的' },
] as const

export default function AppHeader() {
  const navigate = useNavigate()
  const logout = useAuthStore((s) => s.logout)
  const unread = useNotifyStore((s) => s.unreadCount)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header id="app-hdr" className="app-header">
      <NavLink to="/app/tasks" className="logo">
        <span className="dot" />
        <span>CampusHub</span>
      </NavLink>

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
    </header>
  )
}
