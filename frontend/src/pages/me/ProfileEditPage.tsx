import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getMe, updateAcceptLimit, updatePrivacy, updateProfile } from '../../api/user'
import type { PrivacySettings } from '../../types/user'
import { BizError } from '../../types/api'
import '../tasks/Tasks.css'
import './User.css'

const PRIVACY_LABELS: { key: keyof PrivacySettings; label: string; desc: string }[] = [
  { key: 'hidePublishHist',   label: '隐藏我发布的任务', desc: '公开主页不显示发布历史列表' },
  { key: 'hideAcceptHist',    label: '隐藏我接的任务',   desc: '公开主页不显示接单记录' },
  { key: 'hideCourseReviews', label: '隐藏我的课程评价', desc: '我的课评在评价区匿名展示' },
  { key: 'imOpen',            label: '接收私信',         desc: '关闭后陌生人不能给我发私信' },
]

export default function ProfileEditPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()

  const { data: me, isLoading } = useQuery({
    queryKey: ['me'],
    queryFn: () => getMe(),
  })

  const [nickname, setNickname] = useState('')
  const [avatarUrl, setAvatarUrl] = useState('')
  const [acceptLimit, setAcceptLimit] = useState(2)
  const [privacy, setPrivacy] = useState<PrivacySettings>({
    hidePublishHist: true,
    hideAcceptHist: true,
    hideCourseReviews: true,
    imOpen: true,
  })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [savedAt, setSavedAt] = useState<number | null>(null)

  useEffect(() => {
    if (me) {
      setNickname(me.nickname)
      setAvatarUrl(me.avatarUrl ?? '')
      setAcceptLimit(me.dailyAcceptLimit)
      setPrivacy(me.privacy)
    }
  }, [me])

  const profileM  = useMutation({ mutationFn: updateProfile })
  const privacyM  = useMutation({ mutationFn: updatePrivacy })
  const limitM    = useMutation({ mutationFn: updateAcceptLimit })

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!nickname.trim() || nickname.length < 2 || nickname.length > 20) {
      setError('昵称需要 2-20 字')
      return
    }
    if (acceptLimit < 1 || acceptLimit > 3) {
      setError('日接单上限只能 1-3')
      return
    }
    setSaving(true)
    try {
      await profileM.mutateAsync({ nickname: nickname.trim(), avatarUrl: avatarUrl.trim() || undefined })
      await privacyM.mutateAsync(privacy)
      if (me && acceptLimit !== me.dailyAcceptLimit) {
        await limitM.mutateAsync(acceptLimit)
      }
      qc.invalidateQueries({ queryKey: ['me'] })
      setSavedAt(Date.now())
    } catch (err) {
      setError(err instanceof BizError ? err.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  if (isLoading || !me) {
    return <div className="wrap"><div className="task-loading">加载中…</div></div>
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
        <div className="page-sub">个人信息 · 隐私 · 接单上限</div>
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
          <label className="form-label" htmlFor="p-avatar">头像 URL（未来支持上传）</label>
          <input
            id="p-avatar"
            className="form-input"
            type="url"
            value={avatarUrl}
            onChange={(e) => setAvatarUrl(e.target.value)}
            placeholder="https://..."
          />
          <div className="form-hint">留空使用首字母 fallback 头像</div>
        </div>

        <div className="form-field">
          <label className="form-label">手机号</label>
          <input className="form-input" value={me.phoneMasked} disabled />
          <div className="form-hint">出于安全考虑，手机号不可在此修改</div>
        </div>

        <div className="form-field">
          <label className="form-label" htmlFor="p-limit">日接单上限（1-3）</label>
          <input
            id="p-limit"
            className="form-input"
            type="number"
            min={1}
            max={3}
            value={acceptLimit}
            onChange={(e) => setAcceptLimit(parseInt(e.target.value || '1', 10))}
          />
          <div className="form-hint">同时进行中的任务上限，越高越累，按需调整</div>
        </div>

        {/* 隐私 */}
        <div className="form-field">
          <label className="form-label">隐私开关（默认全开）</label>
          <div className="privacy-list" style={{ marginTop: 8 }}>
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
          <div className="form-hint" style={{ color: '#047857' }}>
            ✓ 已保存（{new Date(savedAt).toLocaleTimeString('zh-CN')}）
          </div>
        )}

        <div className="form-submit-row">
          <button
            type="submit"
            className="action-btn action-btn-primary"
            disabled={saving}
            style={{ width: 'auto', minWidth: 200 }}
          >
            {saving ? '保存中…' : '保存修改 →'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/app/me')}
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
