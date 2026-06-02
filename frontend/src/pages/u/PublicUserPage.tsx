import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { getPublicStats } from '../../api/user'
import { startConversation } from '../../api/im'
import { useAuthStore } from '../../stores/auth'
import '../me/User.css'
import '../tasks/Tasks.css'

export default function PublicUserPage() {
  const { userId = '' } = useParams<{ userId: string }>()
  const navigate = useNavigate()
  const myUserId = useAuthStore((s) => s.userId)
  const isLoggedIn = !!useAuthStore((s) => s.accessToken)

  const { data, isLoading, error } = useQuery({
    queryKey: ['public-user', userId],
    queryFn: () => getPublicStats(userId),
    enabled: !!userId,
  })

  const startChat = useMutation({
    mutationFn: (peerId: string) => startConversation(peerId),
    onSuccess: (conv) => navigate(`/app/im/${conv.conversationId}`),
  })

  if (isLoading) return <div className="wrap"><div className="task-loading">加载中…</div></div>
  if (error || !data) {
    return <div className="wrap"><div className="task-error">{(error as Error)?.message ?? '用户不存在'}</div></div>
  }

  const { user, publishedCount, acceptedCount, reviewsCount } = data
  const ch = (user.nickname?.[0] ?? '?').toUpperCase()
  const isMe = myUserId === user.userId
  const allHidden = publishedCount === null && acceptedCount === null && reviewsCount === null

  return (
    <div className="wrap">
      <button onClick={() => navigate(-1)} className="detail-back">
        <span aria-hidden>←</span> 返回
      </button>

      <div className="page-head">
        <h1 className="page-title">
          {user.nickname}<span className="it">.</span>
        </h1>
        <div className="page-sub">{isMe ? '这是你自己的公开页面' : `用户 ID · ${user.userId}`}</div>
      </div>

      <div className="public-user-layout">
        <aside className="public-avatar-card">
          <div className="public-avatar">
            {user.avatarUrl ? <img src={user.avatarUrl} alt={user.nickname} /> : <span>{ch}</span>}
          </div>
          <div className="public-name">{user.nickname}</div>
          {user.verifiedTag && <div className="public-tag">{user.verifiedTag}</div>}

          {!isMe && isLoggedIn && (
            <div className="public-actions">
              <button
                type="button"
                className="me-action-btn is-primary"
                disabled={startChat.isPending}
                onClick={() => startChat.mutate(user.userId)}
              >
                {startChat.isPending ? '打开中…' : '发起私信 →'}
              </button>
              <button type="button" className="me-action-btn is-danger">举报这位用户</button>
            </div>
          )}
          {isMe && (
            <div className="public-actions">
              <Link to="/app/me/edit" className="me-action-btn is-primary">
                编辑资料 →
              </Link>
            </div>
          )}
        </aside>

        <div className="me-card public-stats-card">
          <h3 className="me-card-title" style={{ marginBottom: 4 }}>
            公开<span className="it">活动</span>
          </h3>

          <PublicStatRow
            label={<>发布过的<span className="it"> 任务</span></>}
            count={publishedCount}
          />
          <PublicStatRow
            label={<>接过的<span className="it"> 任务</span></>}
            count={acceptedCount}
          />
          <PublicStatRow
            label={<>课程<span className="it"> 评价</span></>}
            count={reviewsCount}
          />

          {allHidden && (
            <div className="public-privacy-note">
              这位同学开启了完整的<span className="it"> 隐私保护</span>。
              <br />
              CampusHub 后台仍保留实名记录用于仲裁，但前台不展示。
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function PublicStatRow({ label, count }: { label: React.ReactNode; count: number | null }) {
  return (
    <div className="public-stat-row">
      <div className="public-stat-label">{label}</div>
      {count === null ? (
        <span className="public-stat-hidden">已隐藏</span>
      ) : (
        <span className="public-stat-value">{count}</span>
      )}
    </div>
  )
}
