import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createTask } from '../../api/task'
import { useCreditStore } from '../../stores/credit'
import { BizError } from '../../types/api'
import type { TaskCreateDTO, TaskType } from '../../types/task'
import { formatLocalDateTime } from '../../utils/format'
import './Tasks.css'

const TYPE_OPTIONS: { value: TaskType; label: string; tag: string }[] = [
  { value: 'ERRAND',      label: 'errand',  tag: '跑腿' },
  { value: 'MUTUAL_HELP', label: 'help',    tag: '互助' },
  { value: 'TUTOR',       label: 'tutor',   tag: '辅导' },
]

export default function TaskNewPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const creditScore = useCreditStore((s) => s.score)
  const canPublish = useCreditStore((s) => s.canPublish)

  const [type, setType] = useState<TaskType>('ERRAND')
  const [title, setTitle] = useState('')
  const [detail, setDetail] = useState('')
  const [rewardPoint, setRewardPoint] = useState(10)
  const [deadlineAt, setDeadlineAt] = useState(
    // 默认截止时间 = 2 小时后
    formatLocalDateTime(new Date(Date.now() + 2 * 60 * 60_000)),
  )
  const [building, setBuilding] = useState('')
  const [error, setError] = useState('')

  const mutation = useMutation({
    mutationFn: (dto: TaskCreateDTO) => createTask(dto),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['tasks'] })
      navigate(`/app/tasks/${data.taskId}`, { replace: true })
    },
    onError: (err) => {
      setError(err instanceof BizError ? err.message : '发布失败')
    },
  })

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (!title.trim() || title.length > 50) {
      setError('标题需要 1-50 字')
      return
    }
    if (!detail.trim() || detail.length > 500) {
      setError('详情需要 1-500 字')
      return
    }
    if (rewardPoint < 0 || rewardPoint > 500) {
      setError('悬赏 0-500 积分之间')
      return
    }
    if (!deadlineAt) {
      setError('请选择截止时间')
      return
    }
    if (new Date(deadlineAt).getTime() < Date.now()) {
      setError('截止时间必须在未来')
      return
    }
    mutation.mutate({
      taskType: type,
      title: title.trim(),
      detail: detail.trim(),
      rewardPoint,
      deadlineAt: new Date(deadlineAt).toISOString(),
      building: building.trim() || undefined,
    })
  }

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">
          发布<span className="it">新任务</span>。
        </h1>
        <div className="page-sub">F-TASK-01 · 信用闸门 · 冻结悬赏积分</div>
      </div>

      <form className="task-form" onSubmit={handleSubmit} noValidate>
        {/* 类型 */}
        <div className="form-field">
          <label className="form-label">
            任务类型 <span className="required">·</span>
          </label>
          <div className="type-pills">
            {TYPE_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() => setType(opt.value)}
                className={`type-pill${type === opt.value ? ' is-active' : ''}`}
              >
                <span className="it">{opt.label}</span>
                <span>{opt.tag}</span>
              </button>
            ))}
          </div>
        </div>

        {/* 标题 */}
        <div className="form-field">
          <label className="form-label" htmlFor="t-title">
            标题 <span className="required">·</span>
          </label>
          <input
            id="t-title"
            className="form-input"
            placeholder="例如：帮我从南苑食堂带份炒饭"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={50}
          />
          <div className="form-hint">{title.length} / 50</div>
        </div>

        {/* 详情 */}
        <div className="form-field">
          <label className="form-label" htmlFor="t-detail">
            详情 <span className="required">·</span>
          </label>
          <textarea
            id="t-detail"
            className="form-textarea"
            placeholder="详细要求 · 时间地点 · 联系方式（写在 IM 里更安全）"
            value={detail}
            onChange={(e) => setDetail(e.target.value)}
            maxLength={500}
          />
          <div className="form-hint">{detail.length} / 500</div>
        </div>

        {/* 悬赏 + 截止 */}
        <div className="form-row">
          <div className="form-field">
            <label className="form-label" htmlFor="t-reward">
              悬赏积分 <span className="required">·</span>
            </label>
            <input
              id="t-reward"
              className="form-input"
              type="number"
              min={0}
              max={500}
              value={rewardPoint}
              onChange={(e) => setRewardPoint(parseInt(e.target.value || '0', 10))}
            />
            <div className="form-hint">
              当前可用积分：<strong>{useCreditStore.getState().pointBalance}</strong> · 发布后会冻结，完成后划给接单方
            </div>
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="t-deadline">
              截止时间 <span className="required">·</span>
            </label>
            <input
              id="t-deadline"
              className="form-input"
              type="datetime-local"
              value={deadlineAt}
              onChange={(e) => setDeadlineAt(e.target.value)}
            />
            <div className="form-hint">超时未完成将自动进入仲裁队列</div>
          </div>
        </div>

        {/* 楼栋 */}
        <div className="form-field">
          <label className="form-label" htmlFor="t-building">
            地点（可选）
          </label>
          <input
            id="t-building"
            className="form-input"
            placeholder="例如：计科楼 / 仙林食堂 / 北门"
            value={building}
            onChange={(e) => setBuilding(e.target.value)}
            maxLength={50}
          />
        </div>

        {/* 信用闸门 */}
        {!canPublish && (
          <div className="form-error">
            ⚠️ 当前信用分 {creditScore} 低于 60，暂无法发布任务。请完成已接任务以恢复信用。
          </div>
        )}
        {error && <div className="form-error">{error}</div>}

        <div className="form-submit-row">
          <button
            type="submit"
            className="action-btn action-btn-primary"
            disabled={!canPublish || mutation.isPending}
            style={{ width: 'auto', minWidth: 220 }}
          >
            {mutation.isPending ? '发布中…' : '发布任务 →'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/app/tasks')}
            className="action-btn action-btn-ghost"
            style={{ width: 'auto' }}
          >
            取消
          </button>
        </div>
      </form>
    </div>
  )
}
