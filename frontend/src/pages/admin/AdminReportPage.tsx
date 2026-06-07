import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminListReports, adminDecideReport } from '../../api/report'
import type { ReportTargetType } from '../../types/report'
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

const TARGET_LABEL: Record<ReportTargetType, string> = {
  TASK: '任务',
  TRADE: '二手',
  USER: '用户',
}

export default function AdminReportPage() {
  const qc = useQueryClient()
  const { data } = useQuery({ queryKey: ['admin', 'reports'], queryFn: () => adminListReports() })
  const [dialog, setDialog] = useState<DialogConfig | null>(null)
  const [zoom, setZoom] = useState<string | null>(null)

  const decide = useMutation({
    mutationFn: ({ id, decisionType, penaltyPoints, reason }: {
      id: number
      decisionType: 'DISMISS' | 'WARN' | 'PENALIZE'
      penaltyPoints?: number
      reason?: string
    }) => adminDecideReport(id, decisionType, penaltyPoints, reason),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'reports'] }),
  })

  const onPenalize = (id: number) => setDialog({
    title: '扣信用分',
    message: '核实违规后扣减被举报方信用分，将通知双方。',
    field: {
      type: 'number',
      placeholder: '扣多少信用分？(1~50)',
      initial: '10',
      validate: (v) => {
        const n = Number(v)
        return Number.isInteger(n) && n > 0 && n <= 50 ? null : '请输入 1~50 的整数'
      },
    },
    tone: 'danger',
    confirmText: '确认扣分',
    onConfirm: (v) => decide.mutate({ id, decisionType: 'PENALIZE', penaltyPoints: Number(v) }),
  })
  const onWarn = (id: number) => setDialog({
    title: '警告被举报方',
    message: '向被举报方发出平台警告，不扣信用分。',
    field: { type: 'textarea', placeholder: '警告理由（可选）' },
    confirmText: '确认警告',
    onConfirm: (reason) => decide.mutate({ id, decisionType: 'WARN', reason: reason || undefined }),
  })
  const onDismiss = (id: number) => setDialog({
    title: '驳回举报',
    message: '该举报经核实不予支持，将通知举报人。',
    field: { type: 'textarea', placeholder: '驳回理由（可选）' },
    confirmText: '确认驳回',
    onConfirm: (reason) => decide.mutate({ id, decisionType: 'DISMISS', reason: reason || undefined }),
  })

  return (
    <div>
      <h1 className="admin-page-title">举报<span className="it">仲裁</span></h1>
      <div className="admin-page-sub">F-REPORT-01/03 · 驳回 / 警告 / 扣信用分</div>

      {(!data || data.length === 0) && <div className="admin-empty">没有待处理的举报 🎉</div>}
      {data?.map((c) => (
        <div key={c.caseId} className="admin-card">
          <div className="admin-card-head">
            {c.reporter && <PublicUserCard user={c.reporter} size="sm" />}
            <span className="feed-time" style={{ marginLeft: 'auto' }}>{formatRelativeTime(c.createdAt)}</span>
          </div>
          <div className="admin-kv">
            举报对象：<b>{TARGET_LABEL[c.targetType]} #{c.targetId}</b> · 类别：<b>{c.reasonCategory}</b>
          </div>
          {c.description && <div className="admin-kv">说明：{c.description}</div>}
          {c.evidenceUrls.length > 0 && (
            <div className="admin-thumbs">{c.evidenceUrls.map((u) => <img key={u} src={u} alt="证据" onClick={() => setZoom(u)} />)}</div>
          )}
          <div className="admin-actions">
            <button type="button" className="action-btn action-btn-primary" style={{ width: 'auto', minWidth: 110 }}
              disabled={decide.isPending}
              onClick={() => onPenalize(c.caseId)}>
              扣信用分
            </button>
            <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
              disabled={decide.isPending}
              onClick={() => onWarn(c.caseId)}>
              警告
            </button>
            <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
              disabled={decide.isPending}
              onClick={() => onDismiss(c.caseId)}>
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
