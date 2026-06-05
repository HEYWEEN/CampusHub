import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { changePassword, getMe, updatePrivacy, updateProfile } from '../../api/user'
import type { PrivacySettings, UserMeVO } from '../../types/user'
import { BizError } from '../../types/api'
import ImageUploader from '../../components/ImageUploader'
import '../tasks/Tasks.css'
import './User.css'

const PRIVACY_LABELS: { key: keyof PrivacySettings; label: string; desc: string }[] = [
  { key: 'hidePublishHistory', label: '隐藏我发布的任务', desc: '公开主页不显示发布历史列表' },
  { key: 'hideAcceptHistory',  label: '隐藏我接的任务',   desc: '公开主页不显示接单记录' },
  { key: 'hideCourseReviews',  label: '隐藏我收到的评价', desc: '公开主页不显示收到的评价数' },
]

export default function ProfileEditPage() {
  const { data: me, isLoading } = useQuery({
    queryKey: ['me'],
    queryFn: () => getMe(),
  })

  if (isLoading || !me) {
    return <div className="wrap"><div className="task-loading">加载中…</div></div>
  }

  return <ProfileEditForm me={me} />
}

function ProfileEditForm({ me }: { me: UserMeVO }) {
  const navigate = useNavigate()
  const qc = useQueryClient()

  const [nickname, setNickname] = useState(me.nickname)
  const [avatarUrl, setAvatarUrl] = useState(me.avatarUrl ?? '')
  const [privacy, setPrivacy] = useState<PrivacySettings>({
    hidePublishHistory: me.hidePublishHistory,
    hideAcceptHistory: me.hideAcceptHistory,
    hideCourseReviews: me.hideCourseReviews,
  })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [savedAt, setSavedAt] = useState<number | null>(null)

  const profileM  = useMutation({ mutationFn: updateProfile })
  const privacyM  = useMutation({ mutationFn: updatePrivacy })

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!nickname.trim() || nickname.length < 2 || nickname.length > 20) {
      setError('昵称需要 2-20 字')
      return
    }
    setSaving(true)
    try {
      await profileM.mutateAsync({ nickname: nickname.trim(), avatarUrl: avatarUrl.trim() || undefined })
      await privacyM.mutateAsync(privacy)
      qc.invalidateQueries({ queryKey: ['me'] })
      // 资料/隐私改动会影响公开主页（昵称、头像、三项统计的隐藏状态），一并失效缓存
      qc.invalidateQueries({ queryKey: ['public-user'] })
      setSavedAt(Date.now())
    } catch (err) {
      setError(err instanceof BizError ? err.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="wrap">
      <button onClick={() => navigate('/app/me')} className="detail-back">
        <span aria-hidden>←</span> 返回我的主页
      </button>

      <div className="page-head">
        <h1 className="page-title">
          编辑我的<span className="it">资料</span>。
        </h1>
        <div className="page-sub">个人信息 · 隐私</div>
      </div>

      <form className="task-form" onSubmit={handleSave} noValidate>
        {/* 基础信息 */}
        <div className="form-field">
          <label className="form-label" htmlFor="p-nick">
            昵称 <span className="required">·</span>
          </label>
          <input
            id="p-nick"
            className="form-input"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            maxLength={20}
            placeholder="2-20 字，禁敏感词"
          />
        </div>

        <div className="form-field">
          <ImageUploader
            label="头像"
            value={avatarUrl || null}
            onChange={(url) => setAvatarUrl(url ?? '')}
            hint="jpg/png/webp/gif，≤ 5MB；留空使用首字母 fallback 头像"
          />
        </div>

        <div className="form-field">
          <label className="form-label">手机号</label>
          <input className="form-input" value={me.phoneMasked} disabled />
          <div className="form-hint">出于安全考虑，手机号不可在此修改</div>
        </div>

        {/* 隐私 */}
        <div className="form-field">
          <label className="form-label">隐私开关（默认全开）</label>
          <div className="privacy-list form-privacy-list">
            {PRIVACY_LABELS.map((p) => (
              <div className="privacy-item" key={p.key}>
                <div className="privacy-text">
                  <span className="privacy-name">{p.label}</span>
                  <span className="privacy-desc">{p.desc}</span>
                </div>
                <button
                  type="button"
                  className={`toggle${privacy[p.key] ? ' is-on' : ''}`}
                  onClick={() =>
                    setPrivacy((prev) => ({ ...prev, [p.key]: !prev[p.key] }))
                  }
                  aria-label={p.label}
                />
              </div>
            ))}
          </div>
        </div>

        {error && <div className="form-error">{error}</div>}
        {savedAt && !saving && (
          <div className="form-hint is-success">
            ✓ 已保存（{new Date(savedAt).toLocaleTimeString('zh-CN')}）
          </div>
        )}

        <div className="form-submit-row">
          <button
            type="submit"
            className="action-btn action-btn-primary"
            disabled={saving}
          >
            {saving ? '保存中…' : '保存修改 →'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/app/me')}
            className="action-btn action-btn-ghost"
          >
            取消
          </button>
        </div>
      </form>

      <PasswordSection hasPassword={me.hasPassword} />
    </div>
  )
}

const PW_RE = /^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,32}$/

/** 设置（验证码用户首次）/ 修改（已有密码需校验原密码）登录密码。 */
function PasswordSection({ hasPassword }: { hasPassword: boolean }) {
  const qc = useQueryClient()
  const [oldPw, setOldPw] = useState('')
  const [newPw, setNewPw] = useState('')
  const [confirmPw, setConfirmPw] = useState('')
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)

  const m = useMutation({
    mutationFn: () => changePassword(newPw, hasPassword ? oldPw : undefined),
    onSuccess: () => {
      setDone(true)
      setOldPw(''); setNewPw(''); setConfirmPw('')
      qc.invalidateQueries({ queryKey: ['me'] })
    },
    onError: (err) => setError(err instanceof BizError ? err.message : '操作失败'),
  })

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    setError(''); setDone(false)
    if (hasPassword && !oldPw) { setError('请填写原密码'); return }
    if (!PW_RE.test(newPw)) { setError('新密码需 8–32 位，且同时包含字母和数字'); return }
    if (newPw !== confirmPw) { setError('两次输入的新密码不一致'); return }
    m.mutate()
  }

  const title = hasPassword ? '修改密码' : '设置密码'

  return (
    <form className="task-form" onSubmit={submit} noValidate style={{ marginTop: 28 }}>
      <div className="page-head" style={{ marginBottom: 12 }}>
        <h2 className="form-label" style={{ fontSize: 16 }}>{title}</h2>
        <div className="form-hint">
          {hasPassword ? '修改后下次登录请使用新密码。' : '你当前用验证码登录，设置密码后即可用密码登录。'}
        </div>
      </div>

      {hasPassword && (
        <div className="form-field">
          <label className="form-label" htmlFor="pw-old">原密码 <span className="required">·</span></label>
          <input id="pw-old" type="password" className="form-input" autoComplete="current-password"
            value={oldPw} onChange={(e) => setOldPw(e.target.value)} placeholder="请输入当前密码" />
        </div>
      )}

      <div className="form-field">
        <label className="form-label" htmlFor="pw-new">新密码 <span className="required">·</span></label>
        <input id="pw-new" type="password" className="form-input" autoComplete="new-password"
          value={newPw} onChange={(e) => setNewPw(e.target.value)} placeholder="8–32 位，字母+数字" />
      </div>

      <div className="form-field">
        <label className="form-label" htmlFor="pw-confirm">确认新密码 <span className="required">·</span></label>
        <input id="pw-confirm" type="password" className="form-input" autoComplete="new-password"
          value={confirmPw} onChange={(e) => setConfirmPw(e.target.value)} placeholder="再次输入新密码" />
      </div>

      {error && <div className="form-error">{error}</div>}
      {done && <div className="form-hint is-success">✓ 密码已{hasPassword ? '修改' : '设置'}</div>}

      <div className="form-submit-row">
        <button type="submit" className="action-btn action-btn-primary" disabled={m.isPending}>
          {m.isPending ? '提交中…' : `${title} →`}
        </button>
      </div>
    </form>
  )
}
