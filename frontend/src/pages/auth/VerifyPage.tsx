import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  devApproveVerification,
  submitVerification,
  type VerifySubmitDTO,
} from '../../api/auth'
import { useAuthStore } from '../../stores/auth'
import { BizError } from '../../types/api'
import ImageUploader from '../../components/ImageUploader'
import './LoginPage.css'

export default function VerifyPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const verifyStatus = useAuthStore((s) => s.verifyStatus)
  const setVerifyStatus = useAuthStore((s) => s.setVerifyStatus)

  const [realName, setRealName] = useState('')
  const [studentNo, setStudentNo] = useState('')
  const [idCard, setIdCard] = useState('')
  const [attachmentUrls, setAttachmentUrls] = useState<string[]>([])
  const [error, setError] = useState('')

  const submitM = useMutation({
    mutationFn: (dto: VerifySubmitDTO) => submitVerification(dto),
    onSuccess: () => {
      setVerifyStatus('pending')
      qc.invalidateQueries({ queryKey: ['me'] })
    },
    onError: (err) => setError(err instanceof BizError ? err.message : '提交失败'),
  })

  const devApproveM = useMutation({
    mutationFn: () => devApproveVerification(),
    onSuccess: () => {
      setVerifyStatus('approved')
      qc.invalidateQueries({ queryKey: ['me'] })
      navigate('/app/me')
    },
    onError: (err) => setError(err instanceof BizError ? err.message : '一键通过失败'),
  })

  // dev 模式下：用户已提交（status=pending）后显示"模拟通过"按钮
  const isDev = import.meta.env.DEV
  const canDevApprove = isDev && verifyStatus === 'pending'

  // 已提交待审 / 已通过时不再显示填写表单，只展示状态卡片
  const isPending = verifyStatus === 'pending'
  const isApproved = verifyStatus === 'approved'
  const isRejected = verifyStatus === 'rejected'
  const showForm = !isPending && !isApproved

  const goBrowse = () => navigate('/app/tasks')

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (!realName.trim() || realName.length < 2 || realName.length > 30) {
      setError('真实姓名需 2-30 字')
      return
    }
    if (!/^[A-Za-z0-9]{6,20}$/.test(studentNo.trim())) {
      setError('学号格式不正确（6-20 位字母数字）')
      return
    }
    if (idCard && !/^\d{17}[\dXx]$/.test(idCard.trim())) {
      setError('身份证号格式不正确')
      return
    }
    if (attachmentUrls.length === 0) {
      setError('请至少上传 1 张证件图')
      return
    }
    submitM.mutate({
      realName: realName.trim(),
      studentNo: studentNo.trim(),
      idCard: idCard.trim() || undefined,
      attachmentUrls,
    })
  }

  return (
    <main className="login-page">
      <header className="login-header">
        <a href="/" className="logo">
          <img className="logo-mark" src="/logo.png" alt="CampusHub" />
          <span>CampusHub</span>
        </a>
        <button
          onClick={() => navigate('/app/tasks')}
          className="ghost-link"
          style={{ border: 0, background: 'transparent', cursor: 'pointer' }}
        >
          稍后再说 <span aria-hidden>→</span>
        </button>
      </header>

      <section className="login-hero">
        <div className="login-eyebrow">
          <span className="pulse" />
          <span>学生认证 · F-AUTH-03</span>
          <span className="ln" />
        </div>

        <h1 className="login-title">
          One <span className="it">last step.</span>
        </h1>
        <p className="login-sub">
          {isPending
            ? '材料已提交，正在等待管理员审核。'
            : isApproved
              ? '认证已通过，完整功能已解锁。'
              : '上传学生证 / 校园卡照片以激活完整功能。'}
        </p>

        {/* 待审核 / 已通过：展示状态卡片，不再显示填写表单 */}
        {!showForm && (
          <div className="verify-state-card">
            <div className="verify-state-icon" aria-hidden>{isApproved ? '✓' : '⏳'}</div>
            <div className="verify-state-body">
              <div className="verify-state-title">
                {isApproved ? '认证已通过' : '认证审核中'}
              </div>
              <p className="verify-state-desc">
                {isApproved
                  ? '你的身份已通过审核，可以正常使用全部功能。'
                  : '你的认证材料已提交，管理员会尽快处理。审核期间你可以先进去逛逛，通过后将自动解锁发布、接单等完整功能。'}
              </p>
              <div className="verify-actions">
                <button type="button" className="login-submit is-compact" onClick={goBrowse}>
                  <span>{isApproved ? '进入 CampusHub' : '先逛逛'}</span>
                  <span aria-hidden className="login-arrow">→</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {showForm && (
          <form onSubmit={handleSubmit} className="verify-form">
            {isRejected && (
              <div className="login-error">上一次认证未通过，请核对信息后重新提交。</div>
            )}

            <div className="form-field">
              <label className="form-label" htmlFor="v-name">
                真实姓名 <span className="required" aria-hidden>*</span>
              </label>
              <input
                id="v-name"
                className="form-input"
                value={realName}
                onChange={(e) => setRealName(e.target.value)}
                placeholder="证件上的姓名"
                maxLength={30}
              />
            </div>

            <div className="form-field">
              <label className="form-label" htmlFor="v-no">
                学号 <span className="required" aria-hidden>*</span>
              </label>
              <input
                id="v-no"
                className="form-input"
                value={studentNo}
                onChange={(e) => setStudentNo(e.target.value)}
                placeholder="6-20 位"
                maxLength={20}
              />
            </div>

            <div className="form-field">
              <label className="form-label" htmlFor="v-id">身份证号（可选）</label>
              <input
                id="v-id"
                className="form-input"
                value={idCard}
                onChange={(e) => setIdCard(e.target.value)}
                placeholder="18 位"
                maxLength={18}
              />
              <div className="form-hint">仅在出现身份冲突时供管理员核对，AES-GCM 加密落库</div>
            </div>

            <div className="form-field">
              <ImageUploader
                label="证件图（学生证 / 校园卡 / 身份证背面）"
                required
                multiple
                maxCount={5}
                value={attachmentUrls}
                onChange={setAttachmentUrls}
                hint="1-5 张，jpg/png/webp/gif，单张 ≤ 5MB"
              />
            </div>

            <div className="verify-error-slot">
              {error && <div className="login-error">{error}</div>}
            </div>

            <button type="submit" className="login-submit" disabled={submitM.isPending}>
              <span>{submitM.isPending ? '提交中…' : '提交认证'}</span>
              <span aria-hidden className="login-arrow">↗</span>
            </button>
          </form>
        )}

        {/* DEV-only：admin 模块未实现，提供一键自批通过让 dev 能继续测后续流程 */}
        {canDevApprove && (
          <div
            style={{
              marginTop: 24,
              maxWidth: 520,
              padding: 16,
              border: '1px dashed var(--line)',
              borderRadius: 12,
              background: 'var(--accent-softer)',
            }}
          >
            <div style={{ fontFamily: 'var(--font-body)', fontSize: 13, color: 'var(--ink-2)', marginBottom: 8 }}>
              [DEV] admin 模块未实现，无管理员可审批。你可以模拟一次审核通过：
            </div>
            <button
              type="button"
              onClick={() => devApproveM.mutate()}
              disabled={devApproveM.isPending}
              className="login-submit is-compact"
            >
              <span>{devApproveM.isPending ? '处理中…' : '[DEV] 模拟管理员通过 → APPROVED'}</span>
              <span aria-hidden className="login-arrow">↗</span>
            </button>
          </div>
        )}

        {/* 待审/已通过状态下 error 仍可能来自 dev 一键通过失败 */}
        {!showForm && error && (
          <div className="login-error" style={{ marginTop: 16, maxWidth: 520 }}>{error}</div>
        )}
      </section>
    </main>
  )
}
