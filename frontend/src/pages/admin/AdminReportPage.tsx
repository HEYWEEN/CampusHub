import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminListReports, adminDecideReport } from '../../api/report'
import type { ReportTargetType } from '../../types/report'
import PublicUserCard from '../../components/domain/PublicUserCard'
import { formatRelativeTime } from '../../utils/format'

const TARGET_LABEL: Record<ReportTargetType, string> = {
  TASK: '任务',
  TRADE: '二手',
  USER: '用户',
}

export default function AdminReportPage() {
  const qc = useQueryClient()
  const { data } = useQuery({ queryKey: ['admin', 'reports'], queryFn: () => adminListReports() })

  const decide = useMutation({
    mutationFn: ({ id, decisionType, penaltyPoints, reason }: {
      id: number
      decisionType: 'DISMISS' | 'WARN' | 'PENALIZE'
      penaltyPoints?: number
      reason?: string
    }) => adminDecideReport(id, decisionType, penaltyPoints, reason),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'reports'] }),
  })

  const onPenalize = (id: number) => {
    const raw = window.prompt('扣多少信用分？(1~50)', '10')
    if (raw == null) return
    const points = Number(raw)
    if (!Number.isInteger(points) || points <= 0 || points > 50) {
      window.alert('请输入 1~50 的整数')
      return
    }
    decide.mutate({ id, decisionType: 'PENALIZE', penaltyPoints: points, reason: window.prompt('裁决理由（可选）') ?? undefined })
  }

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
            <div className="admin-thumbs">{c.evidenceUrls.map((u) => <img key={u} src={u} alt="证据" />)}</div>
          )}
          <div className="admin-actions">
            <button type="button" className="action-btn action-btn-primary" style={{ width: 'auto', minWidth: 110 }}
              disabled={decide.isPending}
              onClick={() => onPenalize(c.caseId)}>
              扣信用分
            </button>
            <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
              disabled={decide.isPending}
              onClick={() => decide.mutate({ id: c.caseId, decisionType: 'WARN', reason: window.prompt('警告理由（可选）') ?? undefined })}>
              警告
            </button>
            <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
              disabled={decide.isPending}
              onClick={() => decide.mutate({ id: c.caseId, decisionType: 'DISMISS', reason: window.prompt('驳回理由（可选）') ?? undefined })}>
              驳回
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}
