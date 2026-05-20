import { useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import AppHeader from './AppHeader'
import './AppLayout.css'

export default function AppLayout() {
  // 滚动后 header 加 backdrop blur（沿用 HomePage 的 header.scrolled 模式）
  useEffect(() => {
    const hdr = document.getElementById('app-hdr')
    const onScroll = () => hdr?.classList.toggle('scrolled', window.scrollY > 16)
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <div className="app-layout">
      <AppHeader />
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  )
}
