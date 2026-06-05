import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { sendAgentMessage, getAgentHistory } from '../../api/agent'
import { useAuthStore } from '../../stores/auth'
import type { AgentAction } from '../../types/agent'
import TaskCard from '../domain/TaskCard'
import { TASK_TYPE_LABEL } from '../../utils/labels'
import { miniMarkdown } from '../../utils/miniMarkdown'
import './Agent.css'

interface Msg {
  role: 'user' | 'assistant'
  content: string
  actions?: AgentAction[]
}

const GREETING: Msg = {
  role: 'assistant',
  content: '嗨～我是校园助手 🐾 试试「帮我找个取快递的单」或「帮我发个明天取快递给5积分」',
}

const FAB_SIZE = 64
const POS_KEY = 'agent-fab-pos'
// 标记「用户已手动开启新对话」：组件会在 / ↔ /app 间重挂载，
// 没有这个标记的话 load effect 会用 latestHistory 把旧会话拉回来。
const FRESH_KEY = 'campushub.agent.fresh'

function loadPos(): { x: number; y: number } {
  try {
    const s = localStorage.getItem(POS_KEY)
    if (s) return JSON.parse(s)
  } catch { /* ignore */ }
  return { x: window.innerWidth - FAB_SIZE - 26, y: window.innerHeight - FAB_SIZE - 26 }
}

function clamp(x: number, y: number) {
  return {
    x: Math.max(8, Math.min(x, window.innerWidth - FAB_SIZE - 8)),
    y: Math.max(8, Math.min(y, window.innerHeight - FAB_SIZE - 8)),
  }
}

/** 可拖拽的吉祥物悬浮球 + AI 对话面板（仅登录态显示）。 */
export default function AgentWidget() {
  const navigate = useNavigate()
  const accessToken = useAuthStore((s) => s.accessToken)

  const [open, setOpen] = useState(false)
  const [closing, setClosing] = useState(false)
  const [input, setInput] = useState('')
  const [msgs, setMsgs] = useState<Msg[]>([GREETING])
  // 流式打字机：拿到完整 reply 后逐字显示（后端非 SSE，前端模拟流式）
  const [stream, setStream] = useState<{ full: string; shown: number; actions?: AgentAction[] } | null>(null)
  const [convId, setConvId] = useState<number | null>(null)
  const [pos, setPos] = useState(loadPos)
  const [expr, setExpr] = useState<'open' | 'blink' | 'wink' | 'happy'>('open')
  const loadedRef = useRef(false)
  const scrollRef = useRef<HTMLDivElement>(null)
  const drag = useRef<{ sx: number; sy: number; ox: number; oy: number; moved: boolean } | null>(null)

  useEffect(() => {
    if (!open || loadedRef.current) return
    loadedRef.current = true
    // 用户刚开了新对话（还没发消息）→ 保留问候语，别把旧会话拉回来
    if (localStorage.getItem(FRESH_KEY) === '1') return
    getAgentHistory()
      .then((h) => {
        if (h.messages.length > 0) {
          setMsgs(h.messages.map((m) => ({ role: m.role, content: m.content })))
          setConvId(h.conversationId)
        }
      })
      .catch(() => { /* 保留问候语 */ })
  }, [open])

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [msgs, open])

  // 视口缩放时把球夹回可视区
  useEffect(() => {
    const onResize = () => setPos((p) => clamp(p.x, p.y))
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [])

  // 表情插播：默认睁眼，偶尔眨眼 / wink / 笑（异步定时，非 effect 内同步 setState）
  useEffect(() => {
    let alive = true
    let t1: ReturnType<typeof setTimeout>
    let t2: ReturnType<typeof setTimeout>
    const loop = () => {
      t1 = setTimeout(() => {
        if (!alive) return
        const r = Math.random()
        const kind = r < 0.7 ? 'blink' : r < 0.86 ? 'wink' : 'happy'
        const hold = kind === 'happy' ? 1300 : kind === 'wink' ? 520 : 160
        setExpr(kind)
        t2 = setTimeout(() => { if (!alive) return; setExpr('open'); loop() }, hold)
      }, 2800 + Math.random() * 3500)
    }
    loop()
    return () => { alive = false; clearTimeout(t1); clearTimeout(t2) }
  }, [])

  const send = useMutation({
    mutationFn: (text: string) => sendAgentMessage(text, convId),
    onSuccess: (resp) => {
      // 发出首条消息后已是真实会话，清除「新对话」标记（刷新可正常续聊）
      localStorage.removeItem(FRESH_KEY)
      setConvId(resp.conversationId)
      const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
      if (reduce || !resp.reply) {
        // 无障碍：直接落地，不做逐字动画
        setMsgs((p) => [...p, { role: 'assistant', content: resp.reply, actions: resp.actions }])
      } else {
        setStream({ full: resp.reply, shown: 0, actions: resp.actions })
      }
    },
    onError: () => setMsgs((p) => [...p, { role: 'assistant', content: '出错了，请稍后再试 🥲' }]),
  })

  // 打字机推进：每 tick 揭示一批字符；完成后落地为正式消息（带 actions）
  useEffect(() => {
    if (!stream) return
    if (stream.shown >= stream.full.length) {
      setMsgs((p) => [...p, { role: 'assistant', content: stream.full, actions: stream.actions }])
      setStream(null)
      return
    }
    const step = Math.max(1, Math.round(stream.full.length / 80)) // 总时长 ~固定，长文更快
    const t = setTimeout(() => {
      setStream((s) => (s ? { ...s, shown: Math.min(s.full.length, s.shown + step) } : s))
    }, 18)
    return () => clearTimeout(t)
  }, [stream])

  // 流式过程中跟随滚动到底
  useEffect(() => {
    if (stream) scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight })
  }, [stream])

  // 关闭：先播退场动画，动画结束后（onAnimationEnd）再真正卸载面板。
  // reduced-motion 下退场动画被禁用、animationend 不会触发，直接卸载。
  const requestClose = () => {
    if (closing) return
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    if (reduce) { setOpen(false); return }
    setClosing(true)
  }

  const newChat = () => {
    if (send.isPending || stream) return
    localStorage.setItem(FRESH_KEY, '1')
    setMsgs([GREETING])
    setConvId(null)
  }

  if (!accessToken) return null

  // ── 拖拽（指针捕获，拖动则不触发点击） ──
  const onPointerDown = (e: ReactPointerEvent<HTMLButtonElement>) => {
    e.currentTarget.setPointerCapture(e.pointerId)
    drag.current = { sx: e.clientX, sy: e.clientY, ox: pos.x, oy: pos.y, moved: false }
  }
  const onPointerMove = (e: ReactPointerEvent<HTMLButtonElement>) => {
    if (!drag.current) return
    const dx = e.clientX - drag.current.sx
    const dy = e.clientY - drag.current.sy
    if (Math.abs(dx) + Math.abs(dy) > 6) drag.current.moved = true
    setPos(clamp(drag.current.ox + dx, drag.current.oy + dy))
  }
  const onPointerUp = () => {
    if (!drag.current) return
    const moved = drag.current.moved
    drag.current = null
    if (moved) {
      try { localStorage.setItem(POS_KEY, JSON.stringify(pos)) } catch { /* ignore */ }
    } else if (open) {
      requestClose()
    } else {
      setOpen(true)
    }
  }

  const submit = () => {
    const text = input.trim()
    if (!text || send.isPending || stream) return
    setMsgs((p) => [...p, { role: 'user', content: text }])
    setInput('')
    send.mutate(text)
  }

  const goPublish = (a: AgentAction) => {
    if (a.draft) { setOpen(false); navigate('/app/tasks/new', { state: { draft: a.draft } }) }
  }

  return (
    <>
      <button
        type="button"
        className="agent-fab"
        style={{ left: pos.x, top: pos.y }}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        aria-label="AI 助手"
      >
        <svg className={`agent-mascot expr-${expr}`} viewBox="0 0 64 64" aria-hidden>
          {/* 两条竖线大眼睛（白色发光，无底块）+ 笑时出现的小嘴 */}
          <g className="agent-eyes">
            <rect className="eye eye-l" x="16" y="20" width="8.5" height="24" rx="4.25" fill="#fff" />
            <rect className="eye eye-r" x="39.5" y="20" width="8.5" height="24" rx="4.25" fill="#fff" />
          </g>
          <path className="agent-smile" d="M25 47 q7 4 14 0" fill="none" stroke="#fff" strokeWidth="2.4" strokeLinecap="round" />
        </svg>
      </button>

      {(open || closing) && (
        <div
          className={`agent-panel${closing ? ' is-closing' : ''}`}
          onAnimationEnd={(e) => {
            // 仅处理退场动画结束；入场 agent-pop 结束时 closing 为 false
            if (closing && e.animationName === 'agent-pop-out') {
              setClosing(false)
              setOpen(false)
            }
          }}
        >
          <div className="agent-panel-head">
            <span className="agent-panel-title">校园助手</span>
            <div className="agent-panel-tools">
              <button type="button" className="agent-newchat" onClick={newChat} title="开启新对话">+ 新对话</button>
              <button type="button" className="agent-panel-close" onClick={requestClose} aria-label="关闭">×</button>
            </div>
          </div>

          <div className="agent-msgs" ref={scrollRef}>
            {msgs.map((m, i) => (
              <div key={i} className={`agent-row ${m.role}`}>
                {m.role === 'assistant'
                  ? <div className="agent-bubble agent-md" dangerouslySetInnerHTML={{ __html: miniMarkdown(m.content) }} />
                  : <div className="agent-bubble">{m.content}</div>}
                {m.actions?.map((a, j) => (
                  <div key={j} className="agent-action">
                    {a.type === 'task_results' && a.tasks && (
                      <div className="agent-cards">
                        {a.tasks.length === 0
                          ? <div className="agent-empty-tip">暂时没有匹配的任务</div>
                          : a.tasks.map((t) => <TaskCard key={t.taskId} task={t} />)}
                      </div>
                    )}
                    {a.type === 'task_draft' && a.draft && (
                      <div className="agent-draft">
                        <div className="agent-draft-title">{a.draft.title}</div>
                        <div className="agent-draft-meta">
                          {TASK_TYPE_LABEL[a.draft.taskType]} · {a.draft.rewardPoint} 积分
                          {a.draft.deliveryBuilding ? ` · ${a.draft.deliveryBuilding}` : ''}
                        </div>
                        <button type="button" className="agent-draft-go" onClick={() => goPublish(a)}>去发布 →</button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ))}
            {send.isPending && (
              <div className="agent-row assistant">
                <div className="agent-bubble agent-typing" aria-label="正在思考">
                  <span className="agent-dot" /><span className="agent-dot" /><span className="agent-dot" />
                </div>
              </div>
            )}
            {stream && (
              <div className="agent-row assistant">
                <div
                  className="agent-bubble agent-md is-streaming"
                  dangerouslySetInnerHTML={{ __html: miniMarkdown(stream.full.slice(0, stream.shown) + '▍') }}
                />
              </div>
            )}
          </div>

          <div className="agent-input-bar">
            <input
              className="agent-input"
              placeholder="问我找单 / 发单…"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') submit() }}
            />
            <button type="button" className="agent-send" onClick={submit} disabled={send.isPending || !!stream || !input.trim()}>发送</button>
          </div>
        </div>
      )}
    </>
  )
}
