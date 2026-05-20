import { Link } from 'react-router-dom'
import type { PublicUserVO } from '../../types/user'
import './Domain.css'

interface Props {
  user: PublicUserVO
  size?: 'sm' | 'md' | 'lg'
  /** 是否点击跳到 /u/:userId */
  link?: boolean
}

/**
 * ⚠️ 全局唯一渲染 PublicUserVO 的入口（强制规约）
 * 严禁组件内手拼 nickname / avatar — 防止隐私字段泄露
 */
export default function PublicUserCard({ user, size = 'md', link = false }: Props) {
  const ch = (user.nickname?.[0] ?? '?').toUpperCase()

  const inner = (
    <>
      {user.avatarUrl ? (
        <img className={`puc-avatar puc-${size}`} src={user.avatarUrl} alt={user.nickname} />
      ) : (
        <span className={`puc-avatar puc-${size} puc-fallback`} aria-hidden>
          {ch}
        </span>
      )}
      <span className="puc-text">
        <span className="puc-name">{user.nickname}</span>
        {user.verifiedTag && <span className="puc-tag">{user.verifiedTag}</span>}
      </span>
    </>
  )

  if (link) {
    return (
      <Link to={`/u/${user.userId}`} className={`puc puc-row puc-${size}`}>
        {inner}
      </Link>
    )
  }
  return <span className={`puc puc-row puc-${size}`}>{inner}</span>
}
