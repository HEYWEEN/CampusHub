import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listNotifications, markAllAsRead, markAsRead } from '../../api/notify'
import { formatRelativeTime } from '../../utils/format'
import './Notify.css'
import '../tasks/Tasks.css'

type Tab = 'all' | 'unread' | 'read'

// 与后端 appendLetter 实际下发的 type 一一对应（task / auth / credit / team / report / admin）
const TYPE_LABEL: Record<string, { label: string; tone: string }> = {
  TASK_ACCEPTED:        { label: '接单',  tone: 'pending' },
  TASK_COMPLETED:       { label: '完成',  tone: 'done' },
  TASK_CANCELED:        { label: '取消',  tone: 'canceled' },
  TASK_EXPIRED:         { label: '过期',  tone: 'expired' },
  VERIFY_RESULT:        { label: '认证',  tone: 'progress' },
  CREDIT_APPEAL_RESULT: { label: '申诉',  tone: 'wait' },
  TEAM_APPLY_RESULT:    { label: '组队',  tone: 'progress' },
  REPORT_RESULT:        { label: '举报',  tone: 'wait' },
  REPORT_ACTION:        { label: '处罚',  tone: 'expired' },
  ACCOUNT_BAN:          { label: '账号',  tone: 'expired' },
  ROLE_CHANGE:          { label: '权限',  tone: 'progress' },
  TRADE_OFFER_NEW:       { label: '议价',  tone: 'pending' },
  TRADE_OFFER_COUNTERED: { label: '议价',  tone: 'pending' },
  TRADE_OFFER_ACCEPTED:  { label: '成交',  tone: 'done' },
  TRADE_OFFER_REJECTED:  { label: '议价',  tone: 'canceled' },
  TRADE_OFFER_CANCELED:  { label: '议价',  tone: 'canceled' },
  TRADE_ORDER_CANCELED:  { label: '订单',  tone: 'canceled' },
}

// 点通知跳转目标（按 type）。没有的类型则不可点。
const TYPE_LINK: Record<string, string> = {
  TRADE_OFFER_NEW:       '/app/trade/mine',
  TRADE_OFFER_COUNTERED: '/app/trade/mine',
  TRADE_OFFER_ACCEPTED:  '/app/trade/mine',
  TRADE_OFFER_REJECTED:  '/app/trade/mine',
  TRADE_OFFER_CANCELED:  '/app/trade/mine',
  TRADE_ORDER_CANCELED:  '/app/trade/mine',
  TASK_ACCEPTED:         '/app/tasks/my',
  TASK_COMPLETED:        '/app/tasks/my',
  TASK_CANCELED:         '/app/tasks/my',
  TASK_EXPIRED:          '/app/tasks/my',
  TEAM_APPLY_RESULT:     '/app/team',
  CREDIT_APPEAL_RESULT:  '/app/credit',
}

const FALLBACK_META = { label: '通知', tone: 'canceled' }

/**
 * 从 bizId 取业务实体 id。
 * 后端格式 `TYPE:entityId:userId`（如 `TASK_COMPLETED:109:7002`）→ 取第 2 段；
 * 历史/mock 格式 `t109` → 抽数字。无则 null。
 */
function bizEntityId(bizId?: string): string | null {
  if (!bizId) return null
  const parts = bizId.split(':')
  const raw = parts.length >= 2 ? parts[1] : parts[0]
  const digits = raw.replace(/\D/g, '')
  return digits || null
}

export default function NotifyListPage() {
  const [tab, setTab] = useState<Tab>('all')
  const qc = useQueryClient()
  const navigate = useNavigate()

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
          我的<span className="it">消息</span>
        </h1>
        <div className="page-sub">站内通知 · 任务 · 积分 · 系统</div>
      </div>

      <div className="my-tabs notify-tabs-bar">
        <div className="notify-tabs">
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
          className="notify-readall"
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
            const meta = TYPE_LABEL[n.type] ?? FALLBACK_META
            const unread = !n.readAt
            // TASK_* 带 bizId 时直达任务详情（bizKey = TYPE:taskId:userId），否则回退静态映射
            const taskId = n.type.startsWith('TASK_') ? bizEntityId(n.bizId) : null
            const link = taskId ? `/app/tasks/${taskId}` : TYPE_LINK[n.type]
            const go = () => {
              if (!link) return
              if (unread) markM.mutate(n.id)
              navigate(link)
            }
            return (
              <li
                key={n.id}
                className={`notify-item${unread ? ' is-unread' : ''}`}
              >
                <span className={`notify-type status-badge status-${meta.tone}`}>{meta.label}</span>
                <div
                  className={`notify-text${link ? ' is-clickable' : ''}`}
                  onClick={link ? go : undefined}
                  role={link ? 'link' : undefined}
                >
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
