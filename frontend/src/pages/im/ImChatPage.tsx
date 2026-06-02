import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getMessages, listConversations, sendMessage } from '../../api/im'
import type { ImMessageType } from '../../types/im'
import PublicUserCard from '../../components/domain/PublicUserCard'
import ImageUploader from '../../components/ImageUploader'
import { formatRelativeTime } from '../../utils/format'
import './Im.css'

export default function ImChatPage() {
  const { cid = '' } = useParams<{ cid: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [text, setText] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)

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

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length])

  // 进入会话即清未读（拉消息已标读，刷新顶栏徽章）
  useEffect(() => { qc.invalidateQueries({ queryKey: ['im-unread'] }) }, [cid, qc])

  const submitText = () => {
    const t = text.trim()
    if (!t || sendMut.isPending) return
    sendMut.mutate({ content: t, type: 'TEXT' })
  }

  return (
    <div className="wrap im-chat">
      <div className="im-chat-head">
        <button type="button" className="detail-back" onClick={() => navigate('/app/im')}>← 消息</button>
        {peer && <PublicUserCard user={peer} size="sm" link />}
      </div>

      <div className="im-msg-scroll">
        {messages.map((m) => (
          m.contentType === 'SYSTEM' ? (
            <div key={m.messageId} className="im-msg-system">{m.content}</div>
          ) : (
            <div key={m.messageId} className={`im-msg-row${m.mine ? ' mine' : ''}`}>
              <div className="im-bubble">
                {m.contentType === 'IMAGE'
                  ? <img className="im-bubble-img" src={m.content} alt="图片" />
                  : <span>{m.content}</span>}
              </div>
              <span className="im-msg-time">{formatRelativeTime(m.createdAt)}</span>
            </div>
          )
        ))}
        <div ref={bottomRef} />
      </div>

      <div className="im-composer">
        <ImageUploader
          value={null}
          onChange={(url) => { if (url) sendMut.mutate({ content: url, type: 'IMAGE' }) }}
          hint=""
        />
        <input
          className="im-input"
          placeholder="说点什么…"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submitText() } }}
        />
        <button type="button" className="im-send-btn" onClick={submitText} disabled={!text.trim() || sendMut.isPending}>
          发送
        </button>
      </div>
    </div>
  )
}
