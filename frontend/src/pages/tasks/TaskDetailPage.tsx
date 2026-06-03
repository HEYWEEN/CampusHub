import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  acceptTask,
  cancelTask,
  confirmTask,
  getTask,
  submitProof,
} from '../../api/task'
import { useAuthStore } from '../../stores/auth'
import { MOCK_CURRENT_USER_ID } from '../../api/_mock'
import type { TaskDetailVO, TaskStatus } from '../../types/task'
import { BizError } from '../../types/api'
import PublicUserCard from '../../components/domain/PublicUserCard'
import ReportButton from '../../components/domain/ReportButton'
import TaskStatusBadge from '../../components/domain/TaskStatusBadge'
import ImageUploader from '../../components/ImageUploader'
import { formatDeadline } from '../../utils/format'
import { TASK_TYPE_LABEL } from '../../utils/labels'
import './Tasks.css'

// 状态机 5 步定义
const TIMELINE: { status: TaskStatus; label: string }[] = [
  { status: 'PENDING_ACCEPT', label: '已发布' },
  { status: 'IN_PROGRESS',    label: '已接单' },
  { status: 'WAIT_CONFIRM',   label: '凭证已交' },
  { status: 'COMPLETED',      label: '已完成' },
]

function getTimelineState(currentStatus: TaskStatus, step: TaskStatus): 'done' | 'current' | 'pending' {
  const order = ['PENDING_ACCEPT', 'IN_PROGRESS', 'WAIT_CONFIRM', 'COMPLETED']
  const cur = order.indexOf(currentStatus)
  const idx = order.indexOf(step)
  if (cur === idx) return 'current'
  if (cur > idx) return 'done'
  return 'pending'
}

export default function TaskDetailPage() {
  const { id = '' } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const currentUserId = useAuthStore((s) => s.userId) ?? MOCK_CURRENT_USER_ID

  const { data: task, isLoading, error } = useQuery({
    queryKey: ['task', id],
    queryFn: () => getTask(id),
    enabled: !!id,
  })

  const invalidateAll = () => {
    qc.invalidateQueries({ queryKey: ['task', id] })
    qc.invalidateQueries({ queryKey: ['tasks'] })
  }

  const acceptM = useMutation({
    mutationFn: (version: number) => acceptTask(id, version),
    onSuccess: invalidateAll,
  })
  const cancelM = useMutation({
    mutationFn: (reason?: string) => cancelTask(id, reason),
    onSuccess: invalidateAll,
  })
  const confirmM = useMutation({
    mutationFn: () => confirmTask(id),
    onSuccess: invalidateAll,
  })

  if (isLoading) {
    return (
      <div className="wrap">
        <div className="task-loading">正在加载任务…</div>
      </div>
    )
  }

  if (error || !task) {
    return (
      <div className="wrap">
        <div className="task-error">{(error as Error)?.message ?? '任务不存在'}</div>
      </div>
    )
  }

  return (
    <div className="wrap">
      <button onClick={() => navigate(-1)} className="detail-back">
        <span aria-hidden>←</span> 返回任务大厅
      </button>

      <div className="detail-layout">
        {/* 左栏 */}
        <div className="detail-main">
          <div className="detail-tags">
            <span className={`task-type task-type-${task.taskType.toLowerCase()}`}>
              {TASK_TYPE_LABEL[task.taskType]}
            </span>
            <TaskStatusBadge status={task.status} />
          </div>

          <h1 className="detail-title">{task.title}</h1>
          <p className="detail-desc">{task.remark}</p>

          {/* 凭证 — TODO: 后端 TaskDetailVO.attachmentUrls 不带 kind 分类，
              凭证 vs 详情图无法区分；等 P1/P2 阶段补 TaskAttachmentVO 后再恢复 */}
          {task.attachmentUrls && task.attachmentUrls.length > 0 && (
            <div className="detail-proof">
              <div className="detail-proof-title">附件</div>
              <div className="detail-proof-images">
                {task.attachmentUrls.map((url, i) => (
                  <img key={i} src={url} alt={`附件 ${i + 1}`} />
                ))}
              </div>
            </div>
          )}

          {/* 状态机时间线 */}
          {task.status !== 'CANCELED' && task.status !== 'EXPIRED' ? (
            <div className="detail-timeline">
              <div className="timeline-label">流程</div>
              <div className="timeline-steps">
                {TIMELINE.map((step) => {
                  const state = getTimelineState(task.status, step.status)
                  return (
                    <div
                      key={step.status}
                      className={`timeline-step is-${state}`}
                    >
                      <span className="timeline-dot" />
                      <span className="timeline-text">{step.label}</span>
                    </div>
                  )
                })}
              </div>
            </div>
          ) : (
            <div className={`timeline-terminal timeline-terminal-${task.status === 'CANCELED' ? 'canceled' : 'expired'}`}>
              {task.status === 'CANCELED' ? '此任务已取消' : '此任务已超时'}
            </div>
          )}
        </div>

        {/* 右栏 */}
        <aside className="detail-side">
          <div className="side-section">
            <div className="side-label">发布者</div>
            <PublicUserCard user={task.publisher} size="lg" link />
          </div>

          {task.assignee && (
            <div className="side-section">
              <div className="side-label">接单者</div>
              <PublicUserCard user={task.assignee} size="md" link />
            </div>
          )}

          <div className="side-divider" />

          <div className="side-section">
            <div className="side-label">悬赏积分</div>
            <div className="side-reward">
              {task.rewardPoint}<span className="side-reward-unit">积分</span>
            </div>
          </div>

          <div className="side-section">
            <div className="side-label">截止时间</div>
            <div className={`side-deadline${
              formatDeadline(task.deadlineAt).urgent ? ' urgent' : ''
            }${formatDeadline(task.deadlineAt).expired ? ' expired' : ''}`}>
              {formatDeadline(task.deadlineAt).text}
            </div>
            <div style={{ fontSize: 12, color: 'var(--muted)' }}>
              {new Date(task.deadlineAt).toLocaleString('zh-CN', { hour12: false })}
            </div>
          </div>

          {task.deliveryBuilding && (
            <div className="side-section">
              <div className="side-label">送达地点</div>
              <div style={{ fontFamily: 'var(--font-body)', fontSize: 14 }}>{task.deliveryBuilding}</div>
            </div>
          )}
          {task.pickupHint && (
            <div className="side-section">
              <div className="side-label">取件位置</div>
              <div style={{ fontFamily: 'var(--font-body)', fontSize: 14 }}>{task.pickupHint}</div>
            </div>
          )}

          <div className="side-divider" />

          <ActionPanel
            task={task}
            currentUserId={currentUserId}
            onAccept={() => acceptM.mutate(task.version)}
            onCancel={() => cancelM.mutate(undefined)}
            onConfirm={() => confirmM.mutate()}
            onSubmitProof={(images, note) => submitProof(id, images, note).then(invalidateAll)}
            acceptLoading={acceptM.isPending}
            cancelLoading={cancelM.isPending}
            confirmLoading={confirmM.isPending}
            actionError={acceptM.error || cancelM.error || confirmM.error}
          />

          {!task.isPublisher && (
            <div className="side-report">
              <ReportButton targetType="TASK" targetId={Number(task.taskId)} />
            </div>
          )}
        </aside>
      </div>
    </div>
  )
}

// ─── 操作面板：根据状态 + 角色显示不同按钮 ───────────────
function ActionPanel(props: {
  task: TaskDetailVO
  currentUserId: string
  onAccept: () => void
  onCancel: () => void
  onConfirm: () => void
  onSubmitProof: (images: string[], note: string) => Promise<void>
  acceptLoading: boolean
  cancelLoading: boolean
  confirmLoading: boolean
  actionError: Error | null
}) {
  const { task, currentUserId } = props
  const isPublisher = task.publisher.userId === currentUserId
  const isAcceptor  = task.assignee?.userId === currentUserId
  const [showProof, setShowProof] = useState(false)
  const [proofNote, setProofNote] = useState('')
  const [proofImages, setProofImages] = useState<string[]>([])
  const [proofUploading, setProofUploading] = useState(false)
  const [proofErr, setProofErr] = useState('')

  // 终态
  if (task.status === 'COMPLETED' || task.status === 'CANCELED' || task.status === 'EXPIRED') {
    return (
      <button className="action-btn action-btn-ghost" disabled>
        任务已结束
      </button>
    )
  }

  // 发布者视角
  if (isPublisher) {
    if (task.status === 'PENDING_ACCEPT') {
      return (
        <>
          <button onClick={props.onCancel} className="action-btn action-btn-ghost" disabled={props.cancelLoading}>
            {props.cancelLoading ? '取消中…' : '取消任务'}
          </button>
          <p className="action-hint">等待同学接单中</p>
          {props.actionError && <p className="action-hint" style={{ color: 'var(--danger)' }}>{props.actionError.message}</p>}
        </>
      )
    }
    if (task.status === 'WAIT_CONFIRM') {
      return (
        <>
          <button onClick={props.onConfirm} className="action-btn action-btn-primary" disabled={props.confirmLoading}>
            {props.confirmLoading ? '确认中…' : '确认完成 → 结算积分'}
          </button>
          <p className="action-hint">确认前请检查上方凭证</p>
        </>
      )
    }
    return <p className="action-hint">接单方正在处理…</p>
  }

  // 接单者视角
  if (isAcceptor) {
    if (task.status === 'IN_PROGRESS') {
      if (showProof) {
        return (
          <>
            <ImageUploader
              label="凭证图"
              multiple
              maxCount={9}
              value={proofImages}
              onChange={setProofImages}
              hint="jpg/png/webp/gif，单张 ≤ 5MB，1-9 张"
            />
            <textarea
              className="form-textarea"
              placeholder="请简要说明完成情况（最多 300 字）"
              value={proofNote}
              onChange={(e) => setProofNote(e.target.value)}
              maxLength={300}
              style={{ minHeight: 96 }}
            />
            <button
              onClick={async () => {
                if (!proofNote.trim()) {
                  setProofErr('请填写完成说明')
                  return
                }
                if (proofImages.length === 0) {
                  setProofErr('请上传至少 1 张凭证图')
                  return
                }
                setProofErr('')
                setProofUploading(true)
                try {
                  await props.onSubmitProof(proofImages, proofNote)
                  setShowProof(false)
                  setProofNote('')
                  setProofImages([])
                } catch (err) {
                  setProofErr(err instanceof BizError ? err.message : '提交失败')
                } finally {
                  setProofUploading(false)
                }
              }}
              className="action-btn action-btn-primary"
              disabled={proofUploading}
            >
              {proofUploading ? '提交中…' : '提交凭证 → 待发布者确认'}
            </button>
            <button onClick={() => setShowProof(false)} className="action-btn action-btn-ghost" type="button">
              取消
            </button>
            {proofErr && <div className="form-error">{proofErr}</div>}
          </>
        )
      }
      return (
        <>
          <button onClick={() => setShowProof(true)} className="action-btn action-btn-primary">
            上传完成凭证
          </button>
          <p className="action-hint">完成后请提交凭证给发布者确认</p>
        </>
      )
    }
    if (task.status === 'WAIT_CONFIRM') {
      return (
        <>
          <button className="action-btn action-btn-ghost" disabled>等待对方确认</button>
          <p className="action-hint">发布者收到凭证后会确认</p>
        </>
      )
    }
  }

  // 第三方视角（既不是发布者也不是接单者）
  if (task.status === 'PENDING_ACCEPT') {
    return (
      <>
        <button onClick={props.onAccept} className="action-btn action-btn-primary" disabled={props.acceptLoading}>
          {props.acceptLoading ? '接单中…' : `接单 · 完成可得 ${task.rewardPoint} 积分`}
        </button>
        <p className="action-hint">接单后会冻结相同积分作为保证金，完成结算返还</p>
        {props.actionError && <div className="form-error">{props.actionError.message}</div>}
      </>
    )
  }

  return <button className="action-btn action-btn-ghost" disabled>任务进行中</button>
}
