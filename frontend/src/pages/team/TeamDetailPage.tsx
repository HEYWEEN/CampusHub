import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { applyTeam, getRecruit, listApplications, reviewApplication } from '../../api/team'
import { BizError } from '../../types/api'
import PublicUserCard from '../../components/domain/PublicUserCard'
import { TEAM_STATUS_LABEL } from '../../utils/labels'
import { formatRelativeTime } from '../../utils/format'
import '../tasks/Tasks.css'

export default function TeamDetailPage() {
  const { id = '' } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const { data: recruit, isLoading, error: loadErr } = useQuery({
    queryKey: ['team', id],
    queryFn: () => getRecruit(id),
    enabled: !!id,
  })

  const { data: applications } = useQuery({
    queryKey: ['team', id, 'apps'],
    queryFn: () => listApplications(id),
    enabled: !!id && !!recruit?.isCreator,
  })

  const applyMut = useMutation({
    mutationFn: () => applyTeam(id, message.trim()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['team', id] }); setMessage('') },
    onError: (e) => setError(e instanceof BizError ? e.message : '申请失败'),
  })

  const reviewMut = useMutation({
    mutationFn: ({ appId, approve }: { appId: number; approve: boolean }) => reviewApplication(appId, approve),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['team', id, 'apps'] })
      qc.invalidateQueries({ queryKey: ['team', id] })
    },
    onError: (e) => setError(e instanceof BizError ? e.message : '操作失败'),
  })

  if (isLoading) return <div className="wrap"><div className="task-loading">加载中…</div></div>
  if (loadErr || !recruit) {
    return <div className="wrap"><div className="task-error">{(loadErr as Error)?.message ?? '组队帖不存在'}</div></div>
  }

  const st = TEAM_STATUS_LABEL[recruit.status]
  const canApply = recruit.status === 'RECRUITING' && recruit.currentSize < recruit.totalSize

  return (
    <div className="wrap">
      <button type="button" className="detail-back" onClick={() => navigate('/app/team')}>
        ← 返回组队大厅
      </button>

      <div className="detail-layout">
        <div className="detail-main">
          <div className="detail-tags">
            <span className="task-type task-type-team">组队</span>
            <span className={`status-badge status-${st.tone}`}>{st.text}</span>
          </div>
          <h1 className="detail-title">{recruit.title}</h1>

          {recruit.skillTags.length > 0 && (
            <div className="team-tags team-tags-flush">
              {recruit.skillTags.map((t) => <span key={t} className="team-tag">{t}</span>)}
            </div>
          )}

          {recruit.description && <p className="detail-desc">{recruit.description}</p>}

          {/* 队长视角：申请列表 + 审核 */}
          {recruit.isCreator && (
            <div className="team-app-block">
              <div className="side-label">收到的申请 {applications ? `(${applications.length})` : ''}</div>
              {(!applications || applications.length === 0) && (
                <div className="aside-empty">还没有人申请</div>
              )}
              {applications?.map((a) => (
                <div key={a.applicationId} className="team-app">
                  <div className="team-app-head">
                    <PublicUserCard user={a.applicant} size="sm" link />
                    <span className="team-app-credit">信用 {a.creditScore}</span>
                  </div>
                  {a.message && <p className="team-app-msg">{a.message}</p>}
                  <div className="team-app-foot">
                    <span className="feed-time">{formatRelativeTime(a.createdAt)}</span>
                    {a.status === 'PENDING' ? (
                      <span className="team-app-actions">
                        <button type="button" className="action-btn action-btn-primary"
                          disabled={reviewMut.isPending}
                          onClick={() => reviewMut.mutate({ appId: a.applicationId, approve: true })}>同意</button>
                        <button type="button" className="action-btn action-btn-ghost"
                          disabled={reviewMut.isPending}
                          onClick={() => reviewMut.mutate({ appId: a.applicationId, approve: false })}>拒绝</button>
                      </span>
                    ) : (
                      <span className={`status-badge status-${a.status === 'APPROVED' ? 'progress' : 'canceled'}`}>
                        {a.status === 'APPROVED' ? '已通过' : '已拒绝'}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <aside className="detail-side">
          <div className="side-section">
            <div className="side-label">队伍</div>
            <div className="side-reward">
              {recruit.currentSize}<span className="side-reward-unit">/ {recruit.totalSize} 人</span>
            </div>
          </div>
          <div className="side-divider" />
          <div className="side-section">
            <div className="side-label">队长</div>
            <PublicUserCard user={recruit.creator} link />
          </div>

          {/* 申请者视角 */}
          {!recruit.isCreator && (
            <>
              <div className="side-divider" />
              {recruit.myApplicationStatus === 'APPROVED' ? (
                <div className="action-hint">🎉 你已加入这支队伍</div>
              ) : recruit.myApplicationStatus === 'PENDING' ? (
                <div className="action-hint">申请已提交，等待队长审核…</div>
              ) : !canApply ? (
                <div className="action-hint">该组队已结束招募</div>
              ) : (
                <div className="side-section">
                  {recruit.myApplicationStatus === 'REJECTED' && (
                    <div className="action-hint is-spaced">上次申请未通过，可再试一次</div>
                  )}
                  <textarea
                    className="form-textarea team-apply-textarea"
                    placeholder="一句话介绍自己 / 能贡献什么（选填）"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    maxLength={500}
                  />
                  <button
                    type="button"
                    className="action-btn action-btn-primary"
                    disabled={applyMut.isPending}
                    onClick={() => { setError(''); applyMut.mutate() }}
                  >
                    {applyMut.isPending ? '提交中…' : '申请加入'}
                  </button>
                </div>
              )}
            </>
          )}

          {error && <div className="form-error">{error}</div>}
        </aside>
      </div>
    </div>
  )
}
