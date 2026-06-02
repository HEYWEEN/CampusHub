import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApproveVerification, adminListVerifications, adminRejectVerification } from '../../api/admin'
import { formatRelativeTime } from '../../utils/format'

export default function AdminVerifyPage() {
  const qc = useQueryClient()
  const { data } = useQuery({ queryKey: ['admin', 'verifications'], queryFn: () => adminListVerifications() })

  const approve = useMutation({
    mutationFn: (id: number) => adminApproveVerification(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'verifications'] }),
  })
  const reject = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => adminRejectVerification(id, reason),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'verifications'] }),
  })

  const onReject = (id: number) => {
    const reason = window.prompt('驳回理由（≥5 字）')
    if (reason == null) return
    if (reason.trim().length < 5) { window.alert('理由至少 5 字'); return }
    reject.mutate({ id, reason: reason.trim() })
  }

  const busy = approve.isPending || reject.isPending

  return (
    <div>
      <h1 className="admin-page-title">认证<span className="it">审核</span></h1>
      <div className="admin-page-sub">F-ADMIN-01 · 学生证人工审核</div>

      {(!data || data.length === 0) && <div className="admin-empty">没有待审核的认证 🎉</div>}
      {data?.map((v) => (
        <div key={v.verificationId} className="admin-card">
          <div className="admin-card-head">
            <strong>{v.realName ?? '（解密失败）'}</strong>
            <span className="feed-time" style={{ marginLeft: 'auto' }}>{formatRelativeTime(v.createdAt)}</span>
          </div>
          <div className="admin-kv">学号：<b>{v.studentNo ?? '-'}</b> · 用户 ID {v.userId}</div>
          {v.attachmentUrls.length > 0 && (
            <div className="admin-thumbs">
              {v.attachmentUrls.map((u) => <img key={u} src={u} alt="证件" />)}
            </div>
          )}
          <div className="admin-actions">
            <button type="button" className="action-btn action-btn-primary" style={{ width: 'auto', minWidth: 120 }}
              disabled={busy} onClick={() => approve.mutate(v.verificationId)}>通过</button>
            <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
              disabled={busy} onClick={() => onReject(v.verificationId)}>驳回…</button>
          </div>
        </div>
      ))}
    </div>
  )
}
