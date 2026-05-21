import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listNotifications, markAllAsRead, markAsRead } from '../../api/notify'
import { formatRelativeTime } from '../../utils/format'
import './Notify.css'
import '../tasks/Tasks.css'

type Tab = 'all' | 'unread' | 'read'

const TYPE_LABEL: Record<string, { label: string; tone: string }> = {
  TASK_ACCEPTED:  { label: '任务',  tone: 'pending' },
  TASK_PROOF:     { label: '凭证',  tone: 'wait' },
  TASK_REMINDER:  { label: '提醒',  tone: 'expired' },
  CREDIT_SETTLE:  { label: '积分',  tone: 'done' },
  CREDIT_FREEZE:  { label: '积分',  tone: 'progress' },
  REVIEW:         { label: '评价',  tone: 'done' },
  SYSTEM:         { label: '系统',  tone: 'canceled' },
}

export default function NotifyListPage() {
  const [tab, setTab] = useState<Tab>('all')
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['notify', tab],
    queryFn: () => listNotifications(tab),
  })

  const markM = useMutation({
    mutationFn: (id: string) => markAsRead(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notify'] })
      qc.invalidateQueries({ queryKey: ['notify-unread'] })
    },
  })
  const markAllM = useMutation({
    mutationFn: () => markAllAsRead(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notify'] })
      qc.invalidateQueries({ queryKey: ['notify-unread'] })
    },
  })

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">
          有新<span className="it">消息</span>。
        </h1>
        <div className="page-sub">站内通知 · 任务 · 积分 · 系统</div>
      </div>

      <div className="my-tabs" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
        <div style={{ display: 'flex', gap: 32 }}>
          {(['all', 'unread', 'read'] as Tab[]).map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setTab(t)}
              className={`my-tab${tab === t ? ' is-active' : ''}`}
            >
              {t === 'all' ? '全部' : t === 'unread' ? <>未<span className="it">读</span></> : '已读'}
            </button>
          ))}
        </div>
        <button
          type="button"
          onClick={() => markAllM.mutate()}
          className="me-action-btn is-ghost"
          style={{ width: 'auto', marginBottom: 14, padding: '8px 16px' }}
          disabled={markAllM.isPending}
        >
          {markAllM.isPending ? '处理中…' : '全部标为已读'}
        </button>
      </div>

      {isLoading && <div className="task-loading">加载中…</div>}

      {data && data.length === 0 && (
        <div className="task-empty">
          这里很<span className="it">安静。</span> 没有待读消息。
        </div>
      )}

      {data && data.length > 0 && (
        <ul className="notify-list">
          {data.map((n) => {
            const meta = TYPE_LABEL[n.type] ?? { label: n.type, tone: 'canceled' }
            const unread = !n.readAt
            return (
              <li
                key={n.id}
                className={`notify-item${unread ? ' is-unread' : ''}`}
              >
                <span className={`notify-type status-badge status-${meta.tone}`}>{meta.label}</span>
                <div className="notify-text">
                  <div className="notify-title">
                    {unread && <span className="notify-dot" aria-hidden />}
                    {n.title}
                  </div>
                  <div className="notify-body">{n.body}</div>
                  <div className="notify-time">{formatRelativeTime(n.createdAt)}</div>
                </div>
                {unread ? (
                  <button
                    type="button"
                    onClick={() => markM.mutate(n.id)}
                    className="notify-action"
                    disabled={markM.isPending}
                  >
                    标记已读
                  </button>
                ) : (
                  <span className="notify-action-done">已读</span>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
