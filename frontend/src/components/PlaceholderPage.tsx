import type { ReactNode } from 'react'

interface Props {
  /** H1 内容（可含 .it 斜体强调） */
  title: ReactNode
  /** 副标（mono 字体，eyebrow 风格） */
  sub: string
  /** 占位说明（可选，默认显示"即将到来"） */
  body?: ReactNode
}

/**
 * 应用主体页面占位组件
 * — 用于 A 阶段骨架；具体页面在 FE-B~F 阶段逐个替换
 * — 沿用 HomePage tokens 的 editorial 风
 */
export default function PlaceholderPage({ title, sub, body }: Props) {
  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">{title}</h1>
        <div className="page-sub">{sub}</div>
      </div>
      <div className="placeholder">
        {body ?? (
          <>
            即将到来 · <span className="accent">骨架已就位</span>
          </>
        )}
      </div>
    </div>
  )
}
