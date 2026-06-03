import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getMe } from '../../api/user'
import './AdminLayout.css'

const ADMIN_NAV = [
  { to: '/admin/verifications', label: '认证审核' },
  { to: '/admin/users', label: '用户管理' },
  { to: '/admin/appeals', label: '申诉处理' },
  { to: '/admin/reports', label: '举报仲裁' },
]

export default function AdminLayout() {
  const navigate = useNavigate()
  const { data: me, isLoading } = useQuery({ queryKey: ['me'], queryFn: () => getMe(), staleTime: 60_000 })

  if (isLoading) return <div className="admin-shell"><div className="admin-loading">加载中…</div></div>
  if (me && me.role !== 'ADMIN') {
    return (
      <div className="admin-shell">
        <div className="admin-denied">
          <h1>无权访问<span className="it"> 管理后台</span></h1>
          <button type="button" className="action-btn action-btn-primary" style={{ width: 'auto' }}
            onClick={() => navigate('/app/tasks')}>返回 App →</button>
        </div>
      </div>
    )
  }

  return (
    <div className="admin-shell">
      <aside className="admin-side">
        <div className="admin-brand">
          <img className="logo-mark" src="/logo.png" alt="" />
          <span>CampusHub <b>Admin</b></span>
        </div>
        <nav className="admin-nav">
          {ADMIN_NAV.map((n) => (
            <NavLink key={n.to} to={n.to}
              className={({ isActive }) => `admin-nav-link${isActive ? ' is-active' : ''}`}>
              {n.label}
            </NavLink>
          ))}
        </nav>
        <button type="button" className="admin-exit" onClick={() => navigate('/app/tasks')}>← 回到 App</button>
      </aside>
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  )
}
