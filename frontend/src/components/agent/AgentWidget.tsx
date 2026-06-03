import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { sendAgentMessage, getAgentHistory } from '../../api/agent'
import { useAuthStore } from '../../stores/auth'
import type { AgentAction } from '../../types/agent'
import TaskCard from '../domain/TaskCard'
import { TASK_TYPE_LABEL } from '../../utils/labels'
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
  const [input, setInput] = useState('')
  const [msgs, setMsgs] = useState<Msg[]>([GREETING])
  const [pos, setPos] = useState(loadPos)
  const loadedRef = useRef(false)
  const scrollRef = useRef<HTMLDivElement>(null)
  const drag = useRef<{ sx: number; sy: number; ox: number; oy: number; moved: boolean } | null>(null)

  useEffect(() => {
    if (!open || loadedRef.current) return
    loadedRef.current = true
    getAgentHistory()
      .then((h) => { if (h.length > 0) setMsgs(h.map((m) => ({ role: m.role, content: m.content }))) })
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

  const send = useMutation({
    mutationFn: (text: string) => sendAgentMessage(text),
    onSuccess: (resp) => setMsgs((p) => [...p, { role: 'assistant', content: resp.reply, actions: resp.actions }]),
    onError: () => setMsgs((p) => [...p, { role: 'assistant', content: '出错了，请稍后再试 🥲' }]),
  })

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
    } else {
      setOpen((o) => !o)
    }
  }

  const submit = () => {
    const text = input.trim()
    if (!text || send.isPending) return
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
        <svg className="agent-mascot" viewBox="0 0 64 64" aria-hidden>
          {/* 机器人屏幕脸 */}
          <rect x="12" y="16" width="40" height="32" rx="13" fill="#211d1a" />
          {/* 发光竖条眼 */}
          <g className="agent-eyes">
            <rect x="22.5" y="23" width="6.5" height="18" rx="3.25" fill="#fff" />
            <rect x="35" y="23" width="6.5" height="18" rx="3.25" fill="#fff" />
          </g>
        </svg>
      </button>

      {open && (
        <div className="agent-panel">
          <div className="agent-panel-head">
            <span className="agent-panel-title">校园助手</span>
            <button type="button" className="agent-panel-close" onClick={() => setOpen(false)} aria-label="关闭">×</button>
          </div>

          <div className="agent-msgs" ref={scrollRef}>
            {msgs.map((m, i) => (
              <div key={i} className={`agent-row ${m.role}`}>
                <div className="agent-bubble">{m.content}</div>
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
            {send.isPending && <div className="agent-row assistant"><div className="agent-bubble agent-typing">···</div></div>}
          </div>

          <div className="agent-input-bar">
            <input
              className="agent-input"
              placeholder="问我找单 / 发单…"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') submit() }}
            />
            <button type="button" className="agent-send" onClick={submit} disabled={send.isPending || !input.trim()}>发送</button>
          </div>
        </div>
      )}
    </>
  )
}
