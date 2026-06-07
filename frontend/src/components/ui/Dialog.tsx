import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import './Dialog.css'

/** prompt 模式下的输入框配置；不传 field 即为纯确认弹窗。 */
export interface DialogField {
  type?: 'text' | 'textarea' | 'number'
  placeholder?: string
  initial?: string
  /** 返回错误串则拦截确认并就地展示；返回 null 通过。 */
  validate?: (value: string) => string | null
}

export interface DialogProps {
  open: boolean
  title: string
  message?: string
  /** 传入则为「输入弹窗」（替代 window.prompt）；不传则为「确认弹窗」（替代 window.confirm）。 */
  field?: DialogField
  confirmText?: string
  cancelText?: string
  tone?: 'primary' | 'danger'
  /** value 为输入内容（确认模式下为空串）。校验通过后调用，父组件负责关闭。 */
  onConfirm: (value: string) => void
  onCancel: () => void
}

/**
 * 统一的应用内确认/输入弹窗，替换 window.confirm / window.prompt / window.alert。
 * 视觉沿用 ReviewModal 的遮罩 + 面板风格（design tokens）。
 */
export default function Dialog({
  open,
  title,
  message,
  field,
  confirmText = '确认',
  cancelText = '取消',
  tone = 'primary',
  onConfirm,
  onCancel,
}: DialogProps) {
  const [value, setValue] = useState(field?.initial ?? '')
  const [err, setErr] = useState('')
  const inputRef = useRef<HTMLInputElement & HTMLTextAreaElement>(null)

  // 每次打开重置输入与错误，并聚焦
  useEffect(() => {
    if (!open) return
    setValue(field?.initial ?? '')
    setErr('')
    const t = setTimeout(() => inputRef.current?.focus(), 0)
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onCancel() }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      clearTimeout(t)
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  if (!open) return null

  const submit = () => {
    const v = value.trim()
    if (field?.validate) {
      const msg = field.validate(v)
      if (msg) { setErr(msg); return }
    }
    onConfirm(v)
  }

  return createPortal(
    <div className="dlg-overlay" onClick={onCancel}>
      <div className="dlg-panel" onClick={(e) => e.stopPropagation()} role="dialog" aria-label={title}>
        <div className="dlg-head">
          <h2 className="dlg-title">{title}</h2>
          <button type="button" className="dlg-close" onClick={onCancel} aria-label="关闭">×</button>
        </div>

        {message && <p className="dlg-message">{message}</p>}

        {field && (
          field.type === 'textarea' ? (
            <textarea
              ref={inputRef}
              className="dlg-input dlg-textarea"
              placeholder={field.placeholder}
              value={value}
              onChange={(e) => setValue(e.target.value)}
            />
          ) : (
            <input
              ref={inputRef}
              className="dlg-input"
              type={field.type === 'number' ? 'number' : 'text'}
              placeholder={field.placeholder}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); submit() } }}
            />
          )
        )}

        {err && <p className="dlg-err">{err}</p>}

        <div className="dlg-actions">
          <button type="button" className="action-btn action-btn-ghost" onClick={onCancel}>
            {cancelText}
          </button>
          <button
            type="button"
            className={`action-btn ${tone === 'danger' ? 'action-btn-danger' : 'action-btn-primary'}`}
            onClick={submit}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  )
}
