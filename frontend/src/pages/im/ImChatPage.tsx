import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getMessages, listConversations, sendMessage } from '../../api/im'
import type { ImMessageType } from '../../types/im'
import PublicUserCard from '../../components/domain/PublicUserCard'
import ImageUploader from '../../components/ImageUploader'
import { formatRelativeTime } from '../../utils/format'
import { parseOrderCard, orderDetailPath } from '../../utils/orderCard'
import './Im.css'

export default function ImChatPage() {
  const { cid = '' } = useParams<{ cid: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [text, setText] = useState('')
  const scrollRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  // textarea 自增高（上限 140px 后内部滚动）
  const autoGrow = () => {
    const el = inputRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 140)}px`
  }

  // 会话列表（缓存）里找 peer 头像/昵称
  const { data: convs } = useQuery({ queryKey: ['im', 'conversations'], queryFn: () => listConversations() })
  const peer = useMemo(() => convs?.find((c) => c.conversationId === Number(cid))?.peer, [convs, cid])

  const { data } = useQuery({
    queryKey: ['im', 'messages', cid],
    queryFn: () => getMessages(cid),
    enabled: !!cid,
    refetchInterval: 4000,   // 轮询拉新消息（顺带后端标已读）
  })
  // 后端倒序返回，倒回正序（旧→新，底部最新）
  const messages = useMemo(() => [...(data?.items ?? [])].reverse(), [data])

  const sendMut = useMutation({
    mutationFn: ({ content, type }: { content: string; type: ImMessageType }) => sendMessage(cid, content, type),
    onSuccess: () => {
      setText('')
      qc.invalidateQueries({ queryKey: ['im', 'messages', cid] })
      qc.invalidateQueries({ queryKey: ['im', 'conversations'] })
      qc.invalidateQueries({ queryKey: ['im-unread'] })
    },
  })

  // 只滚动内层消息容器，不用 scrollIntoView（它会连带滚动 window 把顶部头部顶出视口）
  useEffect(() => {
    const el = scrollRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages.length])

  // 进入会话即清未读（拉消息已标读，刷新顶栏徽章）
  useEffect(() => { qc.invalidateQueries({ queryKey: ['im-unread'] }) }, [cid, qc])

  const submitText = () => {
    const t = text.trim()
    if (!t || sendMut.isPending) return
    sendMut.mutate({ content: t, type: 'TEXT' })
    // 发送后复位输入框高度
    requestAnimationFrame(() => { if (inputRef.current) inputRef.current.style.height = 'auto' })
  }

  return (
    <div className="wrap im-chat">
      <div className="im-chat-head">
        <button type="button" className="detail-back" onClick={() => navigate('/app/im')}>← 消息</button>
        {peer && <PublicUserCard user={peer} size="sm" link />}
      </div>

      <div className="im-msg-scroll" ref={scrollRef}>
        {messages.map((m) => {
          if (m.contentType === 'SYSTEM') {
            return <div key={m.messageId} className="im-msg-system">{m.content}</div>
          }
          const order = m.contentType === 'ORDER' ? parseOrderCard(m.content) : null
          return (
            <div key={m.messageId} className={`im-msg-row${m.mine ? ' mine' : ''}`}>
              {order ? (
                <Link to={orderDetailPath(order)} className="im-order-card">
                  {order.cover
                    ? <img className="im-order-cover" src={order.cover} alt="" />
                    : <span className="im-order-cover im-order-cover-ph" aria-hidden>{order.kind === 'TASK' ? '务' : '品'}</span>}
                  <div className="im-order-body">
                    <span className="im-order-kind">{order.kind === 'TASK' ? '任务' : '二手'}</span>
                    <span className="im-order-title">{order.title}</span>
                    {order.pricePoint != null && (
                      <span className="im-order-price">{order.pricePoint} 积分</span>
                    )}
                  </div>
                  <span className="im-order-go" aria-hidden>查看详情 →</span>
                </Link>
              ) : (
                <div className="im-bubble">
                  {m.contentType === 'IMAGE'
                    ? <img className="im-bubble-img" src={m.content} alt="图片" />
                    : <span>{m.content}</span>}
                </div>
              )}
              <span className="im-msg-time">{formatRelativeTime(m.createdAt)}</span>
            </div>
          )
        })}
      </div>

      <div className="im-composer">
        <ImageUploader
          value={null}
          onChange={(url) => { if (url) sendMut.mutate({ content: url, type: 'IMAGE' }) }}
          hint=""
        />
        <textarea
          ref={inputRef}
          className="im-input"
          placeholder="说点什么…（Enter 发送，Shift+Enter 换行）"
          rows={1}
          value={text}
          onChange={(e) => { setText(e.target.value); autoGrow() }}
          onKeyDown={(e) => {
            // 输入法候选中（合成态）按 Enter 是「选词」，不能当发送
            if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
              e.preventDefault()
              submitText()
            }
          }}
        />
        <button type="button" className="im-send-btn" onClick={submitText} disabled={!text.trim() || sendMut.isPending}>
          发送
        </button>
      </div>
    </div>
  )
}
