import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminSearchUsers, adminSetBan } from '../../api/admin'
import PublicUserCard from '../../components/domain/PublicUserCard'

export default function AdminUserPage() {
  const qc = useQueryClient()
  const [q, setQ] = useState('')
  const [submitted, setSubmitted] = useState('')

  const { data } = useQuery({
    queryKey: ['admin', 'users', submitted],
    queryFn: () => adminSearchUsers(submitted),
    enabled: submitted.length > 0,
  })

  const ban = useMutation({
    mutationFn: ({ userId, banned }: { userId: number; banned: boolean }) =>
      adminSetBan(userId, banned, banned ? (window.prompt('封禁理由（可选）') ?? undefined) : undefined),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users', submitted] }),
  })

  return (
    <div>
      <h1 className="admin-page-title">用户<span className="it">管理</span></h1>
      <div className="admin-page-sub">F-ADMIN-02 · 搜索 · 封禁 / 解封</div>

      <form className="admin-search" onSubmit={(e) => { e.preventDefault(); setSubmitted(q.trim()) }}>
        <input className="form-input" placeholder="按昵称或用户 ID 搜索…" value={q} onChange={(e) => setQ(e.target.value)} />
        <button type="submit" className="action-btn action-btn-primary" style={{ width: 'auto', minWidth: 90 }}>搜索</button>
      </form>

      {submitted && data && data.length === 0 && <div className="admin-empty">没有匹配的用户</div>}
      {data?.map((u) => (
        <div key={u.userId} className="admin-card">
          <div className="admin-card-row">
            <PublicUserCard user={{ userId: String(u.userId), nickname: u.nickname ?? `用户${u.userId}`, avatarUrl: u.avatarUrl, verifiedTag: u.verifyStatus === 'approved' ? '校园已认证' : null }} size="sm" />
            <span style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
              {u.banned && <span className="status-badge status-canceled">已封禁</span>}
              {u.role === 'ADMIN' && <span className="status-badge status-pending">管理员</span>}
            </span>
          </div>
          <div className="admin-kv">用户 ID {u.userId} · 认证 {u.verifyStatus}</div>
          <div className="admin-actions">
            {u.banned ? (
              <button type="button" className="action-btn action-btn-secondary" style={{ width: 'auto' }}
                disabled={ban.isPending} onClick={() => ban.mutate({ userId: u.userId, banned: false })}>解封</button>
            ) : (
              <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
                disabled={ban.isPending || u.role === 'ADMIN'} onClick={() => ban.mutate({ userId: u.userId, banned: true })}>封禁</button>
            )}
          </div>
        </div>
      ))}
    </div>
  )
}
