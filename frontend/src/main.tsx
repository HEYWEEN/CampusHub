import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './styles/tokens.css'
import './index.css'
import './pages/home/HomePage.css'   // 提到全局：所有页面共享 .pill / .cta-big / .flip 等 utility class
import App from './App.tsx'
import { BizError } from './types/api'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 真实业务错误（4xx）立即报错不重试；网络错/5xx（典型：start.sh 后后端还没起来）
      // 在冷启动窗口内多重试几次，后端就绪后自动恢复，省掉手动刷新
      retry: (failureCount, error) => {
        if (error instanceof BizError && error.httpStatus !== undefined && error.httpStatus < 500) return false
        return failureCount < 8
      },
      retryDelay: (attempt) => Math.min(1500 * 2 ** attempt, 3000), // 1.5s→3s 封顶，8 次≈22s
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
