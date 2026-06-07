import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApproveVerification, adminListVerifications, adminRejectVerification } from '../../api/admin'
import { formatRelativeTime } from '../../utils/format'
import Dialog from '../../components/ui/Dialog'
import Lightbox from '../../components/ui/Lightbox'

export default function AdminVerifyPage() {
  const qc = useQueryClient()
  const { data } = useQuery({ queryKey: ['admin', 'verifications'], queryFn: () => adminListVerifications() })
  const [rejectId, setRejectId] = useState<number | null>(null)
  const [zoom, setZoom] = useState<string | null>(null)

  const approve = useMutation({
    mutationFn: (id: number) => adminApproveVerification(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'verifications'] }),
  })
  const reject = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => adminRejectVerification(id, reason),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'verifications'] }),
  })

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
              {v.attachmentUrls.map((u) => <img key={u} src={u} alt="证件" onClick={() => setZoom(u)} />)}
            </div>
          )}
          <div className="admin-actions">
            <button type="button" className="action-btn action-btn-primary" style={{ width: 'auto', minWidth: 120 }}
              disabled={busy} onClick={() => approve.mutate(v.verificationId)}>通过</button>
            <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
              disabled={busy} onClick={() => setRejectId(v.verificationId)}>驳回…</button>
          </div>
        </div>
      ))}

      <Dialog
        open={rejectId != null}
        title="驳回认证"
        message="请填写驳回理由，将随站内信发送给用户，方便其修改后重新提交。"
        field={{
          type: 'textarea',
          placeholder: '驳回理由（至少 5 字）',
          validate: (v) => (v.length < 5 ? '理由至少 5 字' : null),
        }}
        tone="danger"
        confirmText="确认驳回"
        onConfirm={(reason) => { if (rejectId != null) reject.mutate({ id: rejectId, reason }); setRejectId(null) }}
        onCancel={() => setRejectId(null)}
      />
      <Lightbox src={zoom} onClose={() => setZoom(null)} />
    </div>
  )
}
