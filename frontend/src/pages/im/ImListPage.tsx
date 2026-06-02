import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { listConversations } from '../../api/im'
import PublicUserCard from '../../components/domain/PublicUserCard'
import { formatRelativeTime } from '../../utils/format'
import './Im.css'

function preview(text: string | null, type: string | null): string {
  if (text == null) return '开始聊天吧'
  if (type === 'IMAGE') return '[图片]'
  return text
}

export default function ImListPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['im', 'conversations'],
    queryFn: () => listConversations(),
    refetchInterval: 8000,
  })

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">消<span className="it">息</span>。</h1>
        <div className="page-sub">私信中心 · 任务 / 二手 / 组队 沟通</div>
      </div>

      {isLoading && <div className="task-loading">加载中…</div>}

      {data && data.length === 0 && (
        <div className="task-empty">还没有<span className="it">会话。</span> 去任务或用户主页发起私信吧。</div>
      )}

      {data && data.length > 0 && (
        <ul className="im-conv-list">
          {data.map((c) => (
            <li key={c.conversationId}>
              <Link to={`/app/im/${c.conversationId}`} className="im-conv-row">
                <div className="im-conv-head">
                  <PublicUserCard user={c.peer} size="sm" />
                  <span className="im-conv-time">{formatRelativeTime(c.lastMsgAt)}</span>
                </div>
                <div className="im-conv-bottom">
                  <span className="im-conv-preview">{preview(c.lastMessage, c.lastContentType)}</span>
                  {c.unreadCount > 0 && <span className="im-unread-dot">{c.unreadCount > 99 ? '99+' : c.unreadCount}</span>}
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
