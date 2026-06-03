import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { submitReport } from '../../api/report'
import type { ReportTargetType } from '../../types/report'
import { BizError } from '../../types/api'

const REASONS: { value: string; label: string }[] = [
  { value: 'FRAUD', label: '欺诈 / 诈骗' },
  { value: 'HARASSMENT', label: '骚扰 / 辱骂' },
  { value: 'SPAM', label: '垃圾广告' },
  { value: 'INAPPROPRIATE', label: '不当内容' },
  { value: 'OTHER', label: '其他' },
]

/**
 * 通用举报入口（F-REPORT-01）。点击弹出类别 + 说明，提交到 /api/reports。
 * 调用方自行判断是否渲染（如不对自己发布的对象显示）。
 */
export default function ReportButton({
  targetType,
  targetId,
}: {
  targetType: ReportTargetType
  targetId: number
}) {
  const [open, setOpen] = useState(false)
  const [done, setDone] = useState(false)
  const [category, setCategory] = useState(REASONS[0].value)
  const [description, setDescription] = useState('')

  const submit = useMutation({
    mutationFn: () =>
      submitReport({ targetType, targetId, reasonCategory: category, description: description.trim() || undefined }),
    onSuccess: () => {
      setDone(true)
      setOpen(false)
    },
  })

  if (done) return <span className="report-done">已提交举报，等待平台处理</span>

  return (
    <>
      <button type="button" className="report-link" onClick={() => setOpen(true)}>
        举报
      </button>

      {open && (
        <div className="report-overlay" onClick={() => setOpen(false)}>
          <div className="report-modal" onClick={(e) => e.stopPropagation()}>
            <h3 className="report-modal-title">举报该{targetType === 'USER' ? '用户' : targetType === 'TASK' ? '任务' : '商品'}</h3>

            <label className="report-field-label">违规类别</label>
            <select className="report-select" value={category} onChange={(e) => setCategory(e.target.value)}>
              {REASONS.map((r) => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>

            <label className="report-field-label">补充说明（可选）</label>
            <textarea
              className="report-textarea"
              maxLength={1000}
              rows={4}
              placeholder="描述具体情况，便于管理员核实"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />

            {submit.isError && (
              <p className="report-error">
                {submit.error instanceof BizError ? submit.error.message : '提交失败，请稍后再试'}
              </p>
            )}

            <div className="report-modal-actions">
              <button type="button" className="action-btn action-btn-ghost" style={{ width: 'auto' }}
                onClick={() => setOpen(false)} disabled={submit.isPending}>
                取消
              </button>
              <button type="button" className="action-btn action-btn-primary" style={{ width: 'auto', minWidth: 96 }}
                onClick={() => submit.mutate()} disabled={submit.isPending}>
                {submit.isPending ? '提交中…' : '提交举报'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
