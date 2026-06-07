import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { hideAppeal, hideReceivedReview, listMyAppeals, listReceivedReviews, submitAppeal } from '../../api/credit'
import type { CreditAppealStatus } from '../../types/credit'
import { BizError } from '../../types/api'
import PublicUserCard from '../../components/domain/PublicUserCard'
import ImageUploader from '../../components/ImageUploader'
import Dialog from '../../components/ui/Dialog'
import { formatRelativeTime } from '../../utils/format'
import '../tasks/Tasks.css'
import './Credit.css'

const APPEAL_STATUS: Record<CreditAppealStatus, { text: string; tone: string }> = {
  PENDING: { text: '审核中', tone: 'pending' },
  APPROVED: { text: '已通过', tone: 'progress' },
  REJECTED: { text: '已驳回', tone: 'canceled' },
}

export default function CreditAppealsPage() {
  const qc = useQueryClient()
  const [openId, setOpenId] = useState<number | null>(null)
  const [reason, setReason] = useState('')
  const [evidence, setEvidence] = useState<string[]>([])
  const [error, setError] = useState('')
  const [confirm, setConfirm] = useState<{ title: string; message: string; onOk: () => void } | null>(null)

  const { data: reviews } = useQuery({ queryKey: ['credit', 'received-reviews'], queryFn: () => listReceivedReviews() })
  const { data: appeals } = useQuery({ queryKey: ['credit', 'my-appeals'], queryFn: () => listMyAppeals() })

  const mut = useMutation({
    mutationFn: () => submitAppeal(openId!, reason.trim(), evidence),
    onSuccess: () => {
      setOpenId(null); setReason(''); setEvidence([]); setError('')
      qc.invalidateQueries({ queryKey: ['credit', 'received-reviews'] })
      qc.invalidateQueries({ queryKey: ['credit', 'my-appeals'] })
    },
    onError: (e) => setError(e instanceof BizError ? e.message : '提交失败'),
  })
  const setEvidenceUrls = (u: string[]) => setEvidence(u)

  // 删除（软隐藏）已撤销的差评 / 已处理的申诉记录 —— 仅本人视图
  const hideReviewM = useMutation({
    mutationFn: (reviewId: number) => hideReceivedReview(reviewId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['credit', 'received-reviews'] }),
  })
  const hideAppealM = useMutation({
    mutationFn: (appealId: number) => hideAppeal(appealId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['credit', 'my-appeals'] }),
  })
  const smallBtn = { width: 'auto', height: 34, padding: '0 16px', fontSize: 13 } as const

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">评价<span className="it"> 申诉</span></h1>
        <div className="page-sub">查看收到的评价 · 对差评发起申诉（7 日内 · 同评价 ≤ 3 次 · 管理员裁决）</div>
      </div>

      <section className="appeal-section">
        <h2 className="appeal-h2">我收到的评价</h2>
        {(!reviews || reviews.length === 0) && <div className="task-empty">还没有<span className="it">评价。</span></div>}
        {reviews?.map((r) => (
          <div key={r.reviewId} className="appeal-review">
            <div className="appeal-review-top">
              <PublicUserCard user={r.reviewer} size="sm" />
              <span className="appeal-rating">{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</span>
              <span className="feed-time">{formatRelativeTime(r.createdAt)}</span>
            </div>
            <p className={`appeal-comment${r.voided ? ' is-voided' : ''}`}>{r.comment}</p>
            <div className="appeal-review-foot">
              {r.voided && <span className="status-badge status-canceled">已撤销</span>}
              {r.underAppeal && <span className="status-badge status-pending">申诉中</span>}
              {r.appealable && openId !== r.reviewId && (
                <button type="button" className="action-btn action-btn-secondary" style={smallBtn}
                  onClick={() => { setOpenId(r.reviewId); setReason(''); setEvidence([]); setError('') }}>申诉这条</button>
              )}
              {/* 已撤销的差评可从自己列表里删掉 */}
              {r.voided && (
                <button type="button" className="action-btn action-btn-ghost" style={{ ...smallBtn, marginLeft: 'auto' }}
                  disabled={hideReviewM.isPending}
                  onClick={() => setConfirm({
                    title: '删除这条评价',
                    message: '该评价已撤销，删除后将从你的列表移除，不可恢复。',
                    onOk: () => hideReviewM.mutate(r.reviewId),
                  })}>删除</button>
              )}
            </div>
            {openId === r.reviewId && (
              <div className="appeal-form">
                <textarea className="form-textarea" style={{ minHeight: 90 }} placeholder="说明实际情况（必填）"
                  value={reason} onChange={(e) => setReason(e.target.value)} maxLength={500} />
                <ImageUploader multiple maxCount={5} value={evidence} onChange={setEvidenceUrls} hint="证据图（选填，≤5 张）" />
                {error && <div className="form-error">{error}</div>}
                <div className="form-submit-row" style={{ borderTop: 0, paddingTop: 4 }}>
                  <button type="button" className="action-btn action-btn-primary" style={{ width: 'auto', minWidth: 160 }}
                    disabled={!reason.trim() || mut.isPending} onClick={() => mut.mutate()}>
                    {mut.isPending ? '提交中…' : '提交申诉'}
                  </button>
                  <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }} onClick={() => setOpenId(null)}>取消</button>
                </div>
              </div>
            )}
          </div>
        ))}
      </section>

      <section className="appeal-section">
        <h2 className="appeal-h2">我的申诉</h2>
        {(!appeals || appeals.length === 0) && <div className="appeal-empty">暂无申诉记录</div>}
        {appeals?.map((a) => {
          const st = APPEAL_STATUS[a.status]
          return (
            <div key={a.appealId} className="appeal-item">
              <div className="appeal-item-top">
                <span className={`status-badge status-${st.tone}`}>{st.text}</span>
                <span className="feed-time">{formatRelativeTime(a.createdAt)}</span>
                {/* 已处理（非审核中）的申诉记录可删 */}
                {a.status !== 'PENDING' && (
                  <button type="button" className="action-btn action-btn-ghost" style={{ ...smallBtn, marginLeft: 8 }}
                    disabled={hideAppealM.isPending}
                    onClick={() => setConfirm({
                      title: '删除申诉记录',
                      message: '删除后将从你的列表移除，不可恢复。',
                      onOk: () => hideAppealM.mutate(a.appealId),
                    })}>删除</button>
                )}
              </div>
              <p className="appeal-reason">「{a.reviewComment ?? '原评价'}」 → {a.reason}</p>
              {a.resolveNote && <p className="appeal-note">裁决：{a.resolveNote}</p>}
            </div>
          )
        })}
      </section>

      <Dialog
        open={!!confirm}
        title={confirm?.title ?? ''}
        message={confirm?.message}
        tone="danger"
        confirmText="确认删除"
        onConfirm={() => { confirm?.onOk(); setConfirm(null) }}
        onCancel={() => setConfirm(null)}
      />
    </div>
  )
}
