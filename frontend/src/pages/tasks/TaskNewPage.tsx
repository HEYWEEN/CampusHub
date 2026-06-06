import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createTask } from '../../api/task'
import { useCreditStore } from '../../stores/credit'
import { BizError } from '../../types/api'
import type { TaskCreateDTO, TaskType } from '../../types/task'
import type { TaskDraftVO } from '../../types/agent'
import { formatLocalDateTime } from '../../utils/format'
import './Tasks.css'

const TYPE_OPTIONS: { value: TaskType; label: string; emoji: string }[] = [
  { value: 'ERRAND', label: '跑腿', emoji: '🏃' },
  { value: 'MUTUAL_HELP', label: '互助', emoji: '🤝' },
]

export default function TaskNewPage({ tutor = false }: { tutor?: boolean }) {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [params] = useSearchParams()
  const location = useLocation()
  // 辅导模式：由 /app/edu/tutor/new 直接渲染（tutor prop），或旧的 ?type=TUTOR 入口
  const isTutor = tutor || params.get('type') === 'TUTOR'

  // AI 助手发单草稿（经悬浮球「去发布」带过来），仅作预填，用户仍可改
  const draft = (location.state as { draft?: TaskDraftVO } | null)?.draft

  const creditScore = useCreditStore((s) => s.score)
  const canPublish = useCreditStore((s) => s.canPublish)

  const [type, setType] = useState<TaskType>(draft?.taskType ?? (isTutor ? 'TUTOR' : 'ERRAND'))
  const [title, setTitle] = useState(draft?.title ?? '')
  const [remark, setRemark] = useState(draft?.remark ?? '')
  const [rewardPoint, setRewardPoint] = useState(draft?.rewardPoint ?? (isTutor ? 50 : 10))
  const [deadlineAt, setDeadlineAt] = useState(() =>
    // 草稿带 ISO 时间则用之，否则默认 2 小时后（惰性初始化，避免 render 期间调用 Date.now）
    formatLocalDateTime(draft?.deadlineIso ? new Date(draft.deadlineIso) : new Date(Date.now() + 2 * 60 * 60_000)),
  )
  const [pickupHint, setPickupHint] = useState('')
  const [deliveryBuilding, setDeliveryBuilding] = useState(draft?.deliveryBuilding ?? '')
  const [error, setError] = useState('')

  const mutation = useMutation({
    mutationFn: (dto: TaskCreateDTO) => createTask(dto),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['tasks'] })
      // 辅导单无独立详情页，发布后回辅导大厅；普通任务进任务详情
      navigate(isTutor ? '/app/edu/tutor' : `/app/tasks/${data.taskId}`, { replace: true })
    },
    onError: (err) => {
      setError(err instanceof BizError ? err.message : '发布失败')
    },
  })

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (!title.trim() || title.length > 120) {
      setError('标题需要 1-120 字')
      return
    }
    if (!pickupHint.trim() || pickupHint.length > 200) {
      setError((isTutor ? '上课方式' : '取件位置') + '必填')
      return
    }
    if (!deliveryBuilding.trim() || deliveryBuilding.length > 120) {
      setError((isTutor ? '上课地点' : '送达地点') + '必填')
      return
    }
    if (remark.length > 500) {
      setError('补充说明最多 500 字')
      return
    }
    if (rewardPoint < 1 || rewardPoint > 500) {
      setError('悬赏 1-500 积分之间')
      return
    }
    if (!deadlineAt || new Date(deadlineAt).getTime() < Date.now()) {
      setError('截止时间必须在未来')
      return
    }
    mutation.mutate({
      taskType: type,
      title: title.trim(),
      rewardPoint,
      deadlineAt: new Date(deadlineAt).toISOString(),
      pickupHint: pickupHint.trim(),
      deliveryBuilding: deliveryBuilding.trim(),
      remark: remark.trim() || undefined,
    })
  }

  return (
    <div className="wrap">
      <div className="form-page">
        <div className="form-page-head">
          <h1 className="page-title">
            {isTutor ? <>发布<span className="it">辅导需求</span></> : <>发布<span className="it">新任务</span></>}
          </h1>
          <p className="form-page-sub">
            {isTutor ? '说清课程和时间，约位靠谱的学长学姐。' : '把需求说清楚，靠谱的同学会来接单。'}
          </p>
        </div>

        <form className="task-form form-card" onSubmit={handleSubmit} noValidate>
          {!isTutor && (
            <div className="form-field">
              <label className="form-label">任务类型 <span className="required">·</span></label>
              <div className="type-pills">
                {TYPE_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => setType(opt.value)}
                    className={`type-pill${type === opt.value ? ' is-active' : ''}`}
                  >
                    <span aria-hidden>{opt.emoji}</span>
                    <span>{opt.label}</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="form-field">
            <label className="form-label" htmlFor="t-title">标题 <span className="required">·</span></label>
            <input
              id="t-title"
              className="form-input"
              placeholder={isTutor ? '例如：高数 II 期末冲刺，重点讲二重积分' : '例如：帮我从南苑食堂带份炒饭'}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={50}
            />
            <div className="form-hint">{title.length} / 50</div>
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="t-remark">补充说明（可选）</label>
            <textarea
              id="t-remark"
              className="form-textarea"
              placeholder={isTutor ? '你的目标、基础、可用时间…（联系方式写在 IM 里更安全）' : '详细要求 · 联系方式（写在 IM 里更安全）'}
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
              maxLength={500}
            />
            <div className="form-hint">{remark.length} / 500</div>
          </div>

          <div className="form-row">
            <div className="form-field">
              <label className="form-label" htmlFor="t-reward">悬赏积分 <span className="required">·</span></label>
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
                可用积分：<strong>{useCreditStore.getState().pointBalance}</strong> · 发布后冻结，完成后划给对方
              </div>
            </div>

            <div className="form-field">
              <label className="form-label" htmlFor="t-deadline">截止时间 <span className="required">·</span></label>
              <input
                id="t-deadline"
                className="form-input"
                type="datetime-local"
                value={deadlineAt}
                onChange={(e) => setDeadlineAt(e.target.value)}
              />
              <div className="form-hint">超时未完成将进入仲裁队列</div>
            </div>
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="t-pickup">
              {isTutor ? '上课方式' : '取件位置'} <span className="required">·</span>
            </label>
            <input
              id="t-pickup"
              className="form-input"
              placeholder={isTutor ? '例如：线上腾讯会议 / 线下面授' : '例如：菜鸟驿站 / 南苑食堂窗口 3'}
              value={pickupHint}
              onChange={(e) => setPickupHint(e.target.value)}
              maxLength={200}
            />
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="t-delivery">
              {isTutor ? '上课地点' : '送达地点'} <span className="required">·</span>
            </label>
            <input
              id="t-delivery"
              className="form-input"
              placeholder={isTutor ? '例如：仙林图书馆研讨间 / 线上' : '例如：计科楼 / 启明苑 18 栋'}
              value={deliveryBuilding}
              onChange={(e) => setDeliveryBuilding(e.target.value)}
              maxLength={120}
            />
          </div>

          {!canPublish && (
            <div className="form-error">
              ⚠️ 当前信用分 {creditScore} 低于 60，暂无法发布。请完成已接任务以恢复信用。
            </div>
          )}
          {error && <div className="form-error">{error}</div>}

          <div className="form-submit-row">
            <button
              type="submit"
              className="action-btn action-btn-primary"
              disabled={!canPublish || mutation.isPending}
              style={{ width: 'auto', minWidth: 200 }}
            >
              {mutation.isPending ? '发布中…' : (isTutor ? '发布辅导 →' : '发布任务 →')}
            </button>
            <button
              type="button"
              onClick={() => navigate(isTutor ? '/app/edu/tutor' : '/app/tasks')}
              className="action-btn action-btn-ghost"
              style={{ width: 'auto' }}
            >
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
