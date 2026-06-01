import { useEffect, useRef, useState } from 'react'
import './Select.css'

export interface SelectOption<T extends string> {
  value: T
  label: string
}

interface Props<T extends string> {
  value: T
  options: readonly SelectOption<T>[]
  onChange: (value: T) => void
  ariaLabel?: string
}

/**
 * 自定义下拉（替代原生 <select>，统一 editorial 米色风格）。
 * 点外部 / Esc 关闭，键盘可达。
 */
export default function Select<T extends string>({ value, options, onChange, ariaLabel }: Props<T>) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const current = options.find((o) => o.value === value)

  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  return (
    <div className={`ch-select${open ? ' is-open' : ''}`} ref={ref}>
      <button
        type="button"
        className="ch-select-btn"
        onClick={() => setOpen((o) => !o)}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={ariaLabel}
      >
        <span>{current?.label ?? ''}</span>
        <svg className="ch-select-caret" width="12" height="12" viewBox="0 0 24 24" fill="none"
          stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
          <path d="m6 9 6 6 6-6" />
        </svg>
      </button>
      {open && (
        <ul className="ch-select-menu" role="listbox">
          {options.map((o) => (
            <li
              key={o.value}
              role="option"
              aria-selected={o.value === value}
              className={`ch-select-opt${o.value === value ? ' is-active' : ''}`}
              onClick={() => {
                onChange(o.value)
                setOpen(false)
              }}
            >
              {o.label}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
