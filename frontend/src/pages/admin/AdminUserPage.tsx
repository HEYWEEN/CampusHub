import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminSearchUsers, adminSetBan, adminSetRole } from '../../api/admin'
import { getMe } from '../../api/user'
import PublicUserCard from '../../components/domain/PublicUserCard'
import Dialog, { type DialogField } from '../../components/ui/Dialog'

interface DialogConfig {
  title: string
  message?: string
  field?: DialogField
  tone?: 'primary' | 'danger'
  confirmText?: string
  onConfirm: (value: string) => void
}

export default function AdminUserPage() {
  const qc = useQueryClient()
  const [q, setQ] = useState('')
  const [submitted, setSubmitted] = useState('')
  const [dialog, setDialog] = useState<DialogConfig | null>(null)

  // 当前登录管理员：只有超级管理员才显示「设为/取消管理员」
  const { data: me } = useQuery({ queryKey: ['me'], queryFn: () => getMe(), staleTime: 60_000 })
  const isSuperAdmin = me?.role === 'SUPER_ADMIN'

  // submitted 为空 → 后端返回全部用户；有词则过滤。默认即展示所有用户。
  const { data } = useQuery({
    queryKey: ['admin', 'users', submitted],
    queryFn: () => adminSearchUsers(submitted),
  })

  const ban = useMutation({
    mutationFn: ({ userId, banned, reason }: { userId: number; banned: boolean; reason?: string }) =>
      adminSetBan(userId, banned, reason),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users', submitted] }),
  })

  const role = useMutation({
    mutationFn: ({ userId, admin }: { userId: number; admin: boolean }) => adminSetRole(userId, admin),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users', submitted] }),
  })

  // 封禁：输入理由即确认（bug 10）
  const askBan = (userId: number, nickname: string) => setDialog({
    title: `封禁 ${nickname}`,
    message: '封禁后该用户无法登录使用，可随时解封。',
    field: { type: 'textarea', placeholder: '封禁理由（可选）' },
    tone: 'danger',
    confirmText: '确认封禁',
    onConfirm: (reason) => ban.mutate({ userId, banned: true, reason: reason || undefined }),
  })
  // 解封 / 设管理员 / 取消管理员：二次确认（bug 11）
  const askUnban = (userId: number, nickname: string) => setDialog({
    title: `解封 ${nickname}`,
    message: '解封后该用户可恢复正常登录使用。',
    confirmText: '确认解封',
    onConfirm: () => ban.mutate({ userId, banned: false }),
  })
  const askSetAdmin = (userId: number, nickname: string) => setDialog({
    title: `设为管理员`,
    message: `确认将「${nickname}」设为管理员？对方将获得后台审核 / 仲裁权限。`,
    confirmText: '确认设为管理员',
    onConfirm: () => role.mutate({ userId, admin: true }),
  })
  const askRemoveAdmin = (userId: number, nickname: string) => setDialog({
    title: `取消管理员`,
    message: `确认取消「${nickname}」的管理员权限？`,
    tone: 'danger',
    confirmText: '确认取消',
    onConfirm: () => role.mutate({ userId, admin: false }),
  })

  return (
    <div>
      <h1 className="admin-page-title">用户<span className="it">管理</span></h1>
      <div className="admin-page-sub">F-ADMIN-02 · 搜索 · 封禁 / 解封{isSuperAdmin ? ' · 分派管理员' : ''}</div>

      <form className="admin-search" onSubmit={(e) => { e.preventDefault(); setSubmitted(q.trim()) }}>
        <input className="form-input" placeholder="按昵称或用户 ID 搜索…" value={q} onChange={(e) => setQ(e.target.value)} />
        <button type="submit" className="action-btn action-btn-primary" style={{ width: 'auto', minWidth: 90 }}>搜索</button>
      </form>

      {data && data.length === 0 && <div className="admin-empty">{submitted ? '没有匹配的用户' : '暂无用户'}</div>}
      {data?.map((u) => (
        <div key={u.userId} className="admin-card">
          <div className="admin-card-row">
            <PublicUserCard user={{ userId: String(u.userId), nickname: u.nickname ?? `用户${u.userId}`, avatarUrl: u.avatarUrl, verifiedTag: u.verifyStatus === 'approved' ? '校园已认证' : null }} size="sm" />
            <span style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
              {u.banned && <span className="status-badge status-canceled">已封禁</span>}
              {u.role === 'SUPER_ADMIN' && <span className="status-badge status-pending">超级管理员</span>}
              {u.role === 'ADMIN' && <span className="status-badge status-pending">管理员</span>}
            </span>
          </div>
          <div className="admin-kv">用户 ID {u.userId} · 认证 {u.verifyStatus}</div>
          <div className="admin-actions">
            {u.banned ? (
              <button type="button" className="action-btn action-btn-secondary" style={{ width: 'auto' }}
                disabled={ban.isPending} onClick={() => askUnban(u.userId, u.nickname ?? `用户${u.userId}`)}>解封</button>
            ) : (
              <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
                disabled={ban.isPending || u.role !== 'USER'} onClick={() => askBan(u.userId, u.nickname ?? `用户${u.userId}`)}>封禁</button>
            )}

            {/* 分派管理员：仅超级管理员可见，且不能动超级管理员自身 */}
            {isSuperAdmin && u.role !== 'SUPER_ADMIN' && String(u.userId) !== me?.userId && (
              u.role === 'ADMIN' ? (
                <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
                  disabled={role.isPending} onClick={() => askRemoveAdmin(u.userId, u.nickname ?? `用户${u.userId}`)}>取消管理员</button>
              ) : (
                <button type="button" className="action-btn action-btn-secondary" style={{ width: 'auto' }}
                  disabled={role.isPending} onClick={() => askSetAdmin(u.userId, u.nickname ?? `用户${u.userId}`)}>设为管理员</button>
              )
            )}
          </div>
        </div>
      ))}

      <Dialog
        open={!!dialog}
        title={dialog?.title ?? ''}
        message={dialog?.message}
        field={dialog?.field}
        tone={dialog?.tone}
        confirmText={dialog?.confirmText}
        onConfirm={(v) => { dialog?.onConfirm(v); setDialog(null) }}
        onCancel={() => setDialog(null)}
      />
    </div>
  )
}
