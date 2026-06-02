import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminListAppeals, adminResolveAppeal } from '../../api/admin'
import PublicUserCard from '../../components/domain/PublicUserCard'
import { formatRelativeTime } from '../../utils/format'

export default function AdminAppealPage() {
  const qc = useQueryClient()
  const { data } = useQuery({ queryKey: ['admin', 'appeals'], queryFn: () => adminListAppeals() })

  const resolve = useMutation({
    mutationFn: ({ id, approve, note }: { id: number; approve: boolean; note?: string }) =>
      adminResolveAppeal(id, approve, note),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'appeals'] }),
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
            <div className="admin-thumbs">{a.evidenceUrls.map((u) => <img key={u} src={u} alt="证据" />)}</div>
          )}
          <div className="admin-actions">
            <button type="button" className="action-btn action-btn-primary" style={{ width: 'auto', minWidth: 120 }}
              disabled={resolve.isPending}
              onClick={() => resolve.mutate({ id: a.appealId, approve: true, note: window.prompt('裁决备注（可选）') ?? undefined })}>
              通过（撤销差评）
            </button>
            <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
              disabled={resolve.isPending}
              onClick={() => resolve.mutate({ id: a.appealId, approve: false, note: window.prompt('驳回理由（可选）') ?? undefined })}>
              驳回
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}
