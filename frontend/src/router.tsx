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
import TeamHallPage from './pages/team/TeamHallPage'
import TeamNewPage from './pages/team/TeamNewPage'
import TeamDetailPage from './pages/team/TeamDetailPage'
import ImListPage from './pages/im/ImListPage'
import ImChatPage from './pages/im/ImChatPage'
import NotifyListPage from './pages/notify/NotifyListPage'
import CreditPage from './pages/credit/CreditPage'
import CreditAppealsPage from './pages/credit/CreditAppealsPage'
import AdminLayout from './components/layout/AdminLayout'
import AdminVerifyPage from './pages/admin/AdminVerifyPage'
import AdminUserPage from './pages/admin/AdminUserPage'
import AdminAppealPage from './pages/admin/AdminAppealPage'

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

      // 组队 ✅ F-TEAM-01~04
      { path: 'team',      element: <TeamHallPage /> },
      { path: 'team/new',  element: <TeamNewPage /> },
      { path: 'team/:id',  element: <TeamDetailPage /> },

      // 消息 ✅ F-IM-01/02/04
      { path: 'im',      element: <ImListPage /> },
      { path: 'im/:cid', element: <ImChatPage /> },

      // 通知 ✅ FE-F
      { path: 'notify', element: <NotifyListPage /> },

      // 信用 ✅ FE-F
      { path: 'credit',         element: <CreditPage /> },
      { path: 'credit/appeals', element: <CreditAppealsPage /> },

      // 我的 ✅ FE-D
      { path: 'me',      element: <MePage /> },
      { path: 'me/edit', element: <ProfileEditPage /> },
    ],
  },

  // ===== 管理端 /admin/**（F-ADMIN-01/02 + 申诉处理） =====
  {
    path: '/admin',
    element: (
      <ProtectedRoute>
        <AdminLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <Navigate to="/admin/verifications" replace /> },
      { path: 'verifications', element: <AdminVerifyPage /> },
      { path: 'users',         element: <AdminUserPage /> },
      { path: 'appeals',       element: <AdminAppealPage /> },
    ],
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
