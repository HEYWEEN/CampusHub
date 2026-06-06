import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { useMutation } from '@tanstack/react-query'
import { submitReview } from '../../api/credit'
import { BizError } from '../../types/api'
import './ReviewModal.css'

/**
 * 任务完成后的评价弹窗：1-5 星 + 评论 → POST /api/credit/reviews。
 * 同一任务重复提交后端返回 409，转成「你已评价过该任务」。
 */
export default function ReviewModal({
  open,
  onClose,
  taskId,
  revieweeId,
  revieweeName,
  onSuccess,
}: {
  open: boolean
  onClose: () => void
  taskId: number
  revieweeId: number
  revieweeName: string
  onSuccess?: () => void
}) {
  const [rating, setRating] = useState(5)
  const [hover, setHover] = useState(0)
  const [comment, setComment] = useState('')
  const [err, setErr] = useState('')

  const mut = useMutation({
    mutationFn: () => submitReview(taskId, revieweeId, rating, comment.trim()),
    onSuccess: () => {
      onSuccess?.()
      onClose()
    },
    onError: (e) => setErr(e instanceof BizError ? e.message : '提交失败，请稍后再试'),
  })

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  if (!open) return null

  const shown = hover || rating

  return createPortal(
    <div className="review-overlay" onClick={onClose}>
      <div className="review-panel" onClick={(e) => e.stopPropagation()} role="dialog" aria-label="评价对方">
        <div className="review-head">
          <h2 className="review-title">评价 <span className="it">{revieweeName}</span></h2>
          <button type="button" className="review-close" onClick={onClose} aria-label="关闭">×</button>
        </div>

        <div className="review-stars" role="radiogroup" aria-label="评分">
          {[1, 2, 3, 4, 5].map((n) => (
            <button
              key={n}
              type="button"
              className={`review-star${n <= shown ? ' is-on' : ''}`}
              onMouseEnter={() => setHover(n)}
              onMouseLeave={() => setHover(0)}
              onClick={() => setRating(n)}
              aria-label={`${n} 星`}
            >
              ★
            </button>
          ))}
          <span className="review-stars-label">{shown} 星</span>
        </div>

        <textarea
          className="review-textarea"
          placeholder="说说这次合作的体验（选填，最多 500 字）"
          value={comment}
          maxLength={500}
          onChange={(e) => setComment(e.target.value)}
        />

        {err && <p className="review-err">{err}</p>}

        <div className="review-actions">
          <button type="button" className="action-btn action-btn-ghost" onClick={onClose} disabled={mut.isPending}>
            取消
          </button>
          <button
            type="button"
            className="action-btn action-btn-primary"
            onClick={() => { setErr(''); mut.mutate() }}
            disabled={mut.isPending}
          >
            {mut.isPending ? '提交中…' : '提交评价'}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  )
}
