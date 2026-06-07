import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminListAppeals, adminResolveAppeal } from '../../api/admin'
import PublicUserCard from '../../components/domain/PublicUserCard'
import { formatRelativeTime } from '../../utils/format'
import Dialog, { type DialogField } from '../../components/ui/Dialog'
import Lightbox from '../../components/ui/Lightbox'

interface DialogConfig {
  title: string
  message?: string
  field?: DialogField
  tone?: 'primary' | 'danger'
  confirmText?: string
  onConfirm: (value: string) => void
}

export default function AdminAppealPage() {
  const qc = useQueryClient()
  const { data } = useQuery({ queryKey: ['admin', 'appeals'], queryFn: () => adminListAppeals() })
  const [dialog, setDialog] = useState<DialogConfig | null>(null)
  const [zoom, setZoom] = useState<string | null>(null)

  const resolve = useMutation({
    mutationFn: ({ id, approve, note }: { id: number; approve: boolean; note?: string }) =>
      adminResolveAppeal(id, approve, note),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'appeals'] }),
  })

  const onApprove = (id: number) => setDialog({
    title: '通过申诉（撤销差评）',
    message: '通过后将撤销被申诉的差评，并通知申诉人。',
    field: { type: 'textarea', placeholder: '裁决备注（可选）' },
    confirmText: '确认通过',
    onConfirm: (note) => resolve.mutate({ id, approve: true, note: note || undefined }),
  })
  const onReject = (id: number) => setDialog({
    title: '驳回申诉',
    message: '驳回后保留原差评，并通知申诉人。',
    field: { type: 'textarea', placeholder: '驳回理由（可选）' },
    tone: 'danger',
    confirmText: '确认驳回',
    onConfirm: (note) => resolve.mutate({ id, approve: false, note: note || undefined }),
  })

  return (
    <div>
      <h1 className="admin-page-title">申诉<span className="it">处理</span></h1>
      <div className="admin-page-sub">F-CREDIT-05~07 · 通过则撤销该差评</div>

      {(!data || data.length === 0) && <div className="admin-empty">没有待处理的申诉 🎉</div>}
      {data?.map((a) => (
        <div key={a.appealId} className="admin-card">
          <div className="admin-card-head">
            {a.appellant && <PublicUserCard user={a.appellant} size="sm" />}
            <span className="feed-time" style={{ marginLeft: 'auto' }}>{formatRelativeTime(a.createdAt)}</span>
          </div>
          <div className="admin-kv">被申诉评价（{a.reviewRating ?? '-'}★）：<b>{a.reviewComment ?? '-'}</b></div>
          <div className="admin-kv">申诉理由：{a.reason}</div>
          {a.evidenceUrls.length > 0 && (
            <div className="admin-thumbs">{a.evidenceUrls.map((u) => <img key={u} src={u} alt="证据" onClick={() => setZoom(u)} />)}</div>
          )}
          <div className="admin-actions">
            <button type="button" className="action-btn action-btn-primary" style={{ width: 'auto', minWidth: 120 }}
              disabled={resolve.isPending}
              onClick={() => onApprove(a.appealId)}>
              通过（撤销差评）
            </button>
            <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
              disabled={resolve.isPending}
              onClick={() => onReject(a.appealId)}>
              驳回
            </button>
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
      <Lightbox src={zoom} onClose={() => setZoom(null)} />
    </div>
  )
}
