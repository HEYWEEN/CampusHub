import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import AppHeader from './AppHeader'
import AgentWidget from '../agent/AgentWidget'
import './AppLayout.css'

/**
 * 学生端主壳：AppHeader（自治 sticky）+ Outlet
 * 路由切换为硬切，仅负责把窗口滚回顶部。
 */
export default function AppLayout() {
  const location = useLocation()

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior })
  }, [location.pathname])

  return (
    <div className="app-layout">
      <AppHeader />
      <main className="app-main">
        <div key={location.pathname} className="page-enter">
          <Outlet />
        </div>
      </main>
      <AgentWidget />
    </div>
  )
}
