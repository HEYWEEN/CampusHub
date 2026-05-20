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

  // ===== 公开用户主页 =====
  {
    path: '/u/:userId',
    element: (
      <PlaceholderPage
        title={<>Hello <span className="it">stranger.</span></>}
        sub="公开用户主页 · F-USER-03"
        body={<>仅渲染 PublicUserVO · <span className="accent">FE-D 阶段</span> 实施</>}
      />
    ),
  },

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

      // 二手
      {
        path: 'trade',
        element: (
          <PlaceholderPage
            title={<>宿舍里的<span className="it">好东西</span>。</>}
            sub="二手大厅 · F-TRADE-*"
            body={<><span className="accent">FE-E 阶段</span> 实施</>}
          />
        ),
      },
      {
        path: 'trade/new',
        element: (
          <PlaceholderPage
            title={<>挂个<span className="it">出售</span>。</>}
            sub="F-TRADE-01 · 9 图 EXIF 清洗"
          />
        ),
      },
      {
        path: 'trade/:id',
        element: (
          <PlaceholderPage
            title={<>商品<span className="it">详情</span>。</>}
            sub="F-TRADE-03 · 议价 → 下单 → 冻结积分"
          />
        ),
      },

      // 教育
      {
        path: 'edu/tutor',
        element: (
          <PlaceholderPage
            title={<>找位<span className="it">学长</span>。</>}
            sub="辅导大厅 · F-EDU-06/07"
          />
        ),
      },
      {
        path: 'edu/tutor/new',
        element: (
          <PlaceholderPage
            title={<>发布<span className="it">辅导需求</span>。</>}
            sub="F-EDU-05 · 违禁词拦截 + 信用闸门"
          />
        ),
      },
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

      // 通知
      {
        path: 'notify',
        element: (
          <PlaceholderPage
            title={<>有<span className="it">新</span>消息。</>}
            sub="站内通知 · F-NOTIFY-02/03"
          />
        ),
      },

      // 信用
      {
        path: 'credit',
        element: (
          <PlaceholderPage
            title={<>我的<span className="it">信用</span>。</>}
            sub="信用分 + 积分账户 · F-CREDIT-01/08"
            body={<>历史曲线 · 流水查询 · <span className="accent">FE-F 阶段</span></>}
          />
        ),
      },
      {
        path: 'credit/appeals',
        element: (
          <PlaceholderPage
            title={<>申诉<span className="it">记录</span>。</>}
            sub="F-CREDIT-05~07"
          />
        ),
      },

      // 我的
      {
        path: 'me',
        element: (
          <PlaceholderPage
            title={<>我的<span className="it">主页</span>。</>}
            sub="个人资料 + 隐私开关 · F-USER-01/02/04"
          />
        ),
      },
      {
        path: 'me/edit',
        element: (
          <PlaceholderPage
            title={<>编辑<span className="it">资料</span>。</>}
            sub="昵称 / 头像 / 三项隐私开关默认开"
          />
        ),
      },
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
