import { useEffect, useRef } from 'react'
import { RouterProvider } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { router } from './router'
import { useAuthStore } from './stores/auth'

export default function App() {
  const userId = useAuthStore((s) => s.userId)
  const qc = useQueryClient()
  const prevUserId = useRef(userId)

  // 换号 / 登出时清空 React Query 缓存：
  // 否则上一个用户缓存的会话/消息（含按其视角算的 mine 标记）会串给新用户。
  useEffect(() => {
    if (prevUserId.current !== userId) {
      qc.clear()
      prevUserId.current = userId
    }
  }, [userId, qc])

  return <RouterProvider router={router} />
}
