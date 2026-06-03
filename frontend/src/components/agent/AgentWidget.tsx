import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { sendAgentMessage, getAgentHistory } from '../../api/agent'
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

export default function AgentWidget() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const [msgs, setMsgs] = useState<Msg[]>([GREETING])
  const loadedRef = useRef(false)
  const scrollRef = useRef<HTMLDivElement>(null)

  // 首次打开拉历史（失败/空则保留问候语）。用 ref 作守卫，避免 effect 内同步 setState
  useEffect(() => {
    if (!open || loadedRef.current) return
    loadedRef.current = true
    getAgentHistory()
      .then((h) => {
        if (h.length > 0) setMsgs(h.map((m) => ({ role: m.role, content: m.content })))
      })
      .catch(() => { /* 保留问候语 */ })
  }, [open])

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [msgs, open])

  const send = useMutation({
    mutationFn: (text: string) => sendAgentMessage(text),
    onSuccess: (resp) => {
      setMsgs((prev) => [...prev, { role: 'assistant', content: resp.reply, actions: resp.actions }])
    },
    onError: () => {
      setMsgs((prev) => [...prev, { role: 'assistant', content: '出错了，请稍后再试 🥲' }])
    },
  })

  const submit = () => {
    const text = input.trim()
    if (!text || send.isPending) return
    setMsgs((prev) => [...prev, { role: 'user', content: text }])
    setInput('')
    send.mutate(text)
  }

  const goPublish = (action: AgentAction) => {
    if (action.draft) {
      setOpen(false)
      navigate('/app/tasks/new', { state: { draft: action.draft } })
    }
  }

  return (
    <>
      <button
        type="button"
        className={`agent-fab${open ? ' is-open' : ''}`}
        onClick={() => setOpen((o) => !o)}
        aria-label="AI 助手"
      >
        <span className="agent-fab-face" aria-hidden>{open ? '×' : '🐾'}</span>
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
                        <button type="button" className="agent-draft-go" onClick={() => goPublish(a)}>
                          去发布 →
                        </button>
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
            <button type="button" className="agent-send" onClick={submit} disabled={send.isPending || !input.trim()}>
              发送
            </button>
          </div>
        </div>
      )}
    </>
  )
}
