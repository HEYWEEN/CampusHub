import { Outlet } from 'react-router-dom'
import AppHeader from './AppHeader'
import './AppLayout.css'

/**
 * 学生端主壳：AppHeader（自治 sticky）+ Outlet
 * AppHeader 内部已处理 sticky / 未读拉取 / 登出 — 这里不再 useEffect
 */
export default function AppLayout() {
  return (
    <div className="app-layout">
      <AppHeader />
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  )
}
