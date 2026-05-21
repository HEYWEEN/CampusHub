import { createBrowserRouter, Navigate } from 'react-router-dom'
import HomePage from './pages/home/HomePage'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import VerifyPage from './pages/auth/VerifyPage'
import AppLayout from './components/layout/AppLayout'
import ProtectedRoute from './components/ProtectedRoute'
import PlaceholderPage from './components/PlaceholderPage'
import TaskHallPage from './pages/tasks/TaskHallPage'
import TaskDetailPage from './pages/tasks/TaskDetailPage'
import TaskNewPage from './pages/tasks/TaskNewPage'
import MyTasksPage from './pages/tasks/MyTasksPage'
import MePage from './pages/me/MePage'
import ProfileEditPage from './pages/me/ProfileEditPage'
import PublicUserPage from './pages/u/PublicUserPage'
import TradeHallPage from './pages/trade/TradeHallPage'
import TradeDetailPage from './pages/trade/TradeDetailPage'
import TradeNewPage from './pages/trade/TradeNewPage'
import EduTutorHallPage from './pages/edu/EduTutorHallPage'
import EduTutorNewPage from './pages/edu/EduTutorNewPage'
import NotifyListPage from './pages/notify/NotifyListPage'
import CreditPage from './pages/credit/CreditPage'

/**
 * 路由树（与 docs/P4/04_前端架构设计.md §二 对齐）
 * — 公开路由：/ /login /register /verify /u/:userId
 * — 学生端：/app/**（ProtectedRoute + AppLayout）
 * — 管理端：/admin/**（后续 FE-F 阶段加 AdminLayout）
 */
export const router = createBrowserRouter([
  // ===== 公开路由 =====
  { path: '/', element: <HomePage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/verify', element: <VerifyPage /> },

  // ===== 公开用户主页 ✅ FE-D =====
  { path: '/u/:userId', element: <PublicUserPage /> },

  // ===== 学生端 /app/** =====
  {
    path: '/app',
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <Navigate to="/app/tasks" replace /> },

      // 任务 ✅ FE-C 已实施
      { path: 'tasks',      element: <TaskHallPage /> },
      { path: 'tasks/new',  element: <TaskNewPage /> },
      { path: 'tasks/my',   element: <MyTasksPage /> },
      { path: 'tasks/:id',  element: <TaskDetailPage /> },

      // 二手 ✅ FE-E
      { path: 'trade',      element: <TradeHallPage /> },
      { path: 'trade/new',  element: <TradeNewPage /> },
      { path: 'trade/:id',  element: <TradeDetailPage /> },

      // 教育 ✅ FE-E
      { path: 'edu/tutor',     element: <EduTutorHallPage /> },
      { path: 'edu/tutor/new', element: <EduTutorNewPage /> },
      {
        path: 'edu/resources',
        element: (
          <PlaceholderPage
            title={<>学习<span className="it">资料</span>。</>}
            sub="F-EDU-01~04 · P1 优先级"
          />
        ),
      },

      // 组队
      {
        path: 'team',
        element: (
          <PlaceholderPage
            title={<>找几个<span className="it">队友</span>。</>}
            sub="比赛 / 课设组队 · F-TEAM-* · P1"
          />
        ),
      },

      // 消息
      {
        path: 'im',
        element: (
          <PlaceholderPage
            title={<>聊一<span className="it">聊</span>。</>}
            sub="私信中心 · F-IM-*"
          />
        ),
      },
      {
        path: 'im/:cid',
        element: (
          <PlaceholderPage
            title={<>具体<span className="it">会话</span>。</>}
            sub="F-IM-02 · 文本 / 图片 / 地点卡片"
          />
        ),
      },

      // 通知 ✅ FE-F
      { path: 'notify', element: <NotifyListPage /> },

      // 信用 ✅ FE-F
      { path: 'credit',         element: <CreditPage /> },
      {
        path: 'credit/appeals',
        element: (
          <PlaceholderPage
            title={<>申诉<span className="it">记录</span>。</>}
            sub="F-CREDIT-05~07 · 联调阶段补"
          />
        ),
      },

      // 我的 ✅ FE-D
      { path: 'me',      element: <MePage /> },
      { path: 'me/edit', element: <ProfileEditPage /> },
    ],
  },

  // ===== 管理端 /admin/**（FE-F 阶段实施 AdminLayout） =====
  {
    path: '/admin/*',
    element: (
      <PlaceholderPage
        title={<>Admin <span className="it">console.</span></>}
        sub="管理后台 · F-ADMIN-* / F-REPORT-*"
        body={<>仲裁 · 认证审核 · 用户管理 · <span className="accent">FE-F 阶段</span></>}
      />
    ),
  },

  // ===== 404 兜底 =====
  {
    path: '*',
    element: (
      <PlaceholderPage
        title={<>找不到 <span className="it">这一页</span>。</>}
        sub="404 · 这条路 CampusHub 还没修"
        body={
          <>
            <a href="/" className="accent" style={{ color: 'var(--accent)' }}>
              返回首页 →
            </a>
          </>
        }
      />
    ),
  },
])
