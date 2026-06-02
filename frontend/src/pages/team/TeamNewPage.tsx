import { useState, type FormEvent, type KeyboardEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createRecruit } from '../../api/team'
import type { TeamRecruitCreateDTO, TeamRecruitVO } from '../../types/team'
import { BizError } from '../../types/api'
import '../tasks/Tasks.css'

export default function TeamNewPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [tags, setTags] = useState<string[]>([])
  const [tagInput, setTagInput] = useState('')
  const [totalSize, setTotalSize] = useState(3)
  const [error, setError] = useState('')

  const addTag = (raw: string) => {
    const t = raw.trim().replace(/[,，]/g, '')
    if (!t) return
    if (tags.length >= 5) { setError('技能标签最多 5 个'); return }
    if (tags.includes(t)) return
    setTags([...tags, t])
    setTagInput('')
    setError('')
  }

  const onTagKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',' || e.key === '，') {
      e.preventDefault()
      addTag(tagInput)
    } else if (e.key === 'Backspace' && !tagInput && tags.length) {
      setTags(tags.slice(0, -1))
    }
  }

  const mutation = useMutation({
    mutationFn: (dto: TeamRecruitCreateDTO) => createRecruit(dto),
    onSuccess: (r: TeamRecruitVO) => {
      qc.invalidateQueries({ queryKey: ['teams'] })
      navigate(`/app/team/${r.recruitId}`, { replace: true })
    },
    onError: (err) => setError(err instanceof BizError ? err.message : '发布失败'),
  })

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setError('')
    const finalTags = tagInput.trim() ? [...tags, tagInput.trim()] : tags
    if (!title.trim() || title.length > 120) { setError('标题需 1-120 字'); return }
    if (finalTags.length < 1 || finalTags.length > 5) { setError('技能标签需 1~5 个'); return }
    if (totalSize < 2 || totalSize > 50) { setError('队伍总人数 2~50 人'); return }
    mutation.mutate({
      title: title.trim(),
      description: description.trim() || undefined,
      skillTags: finalTags,
      totalSize,
    })
  }

  return (
    <div className="wrap">
      <div className="form-page">
        <div className="form-page-head">
          <h1 className="page-title">发布<span className="it">组队</span>。</h1>
          <p className="form-page-sub">说清要什么人、要几个人，让对口的同学找上门。</p>
        </div>

        <form className="task-form form-card" onSubmit={handleSubmit} noValidate>
          <div className="form-field">
            <label className="form-label" htmlFor="tm-title">标题 <span className="required">·</span></label>
            <input
              id="tm-title"
              className="form-input"
              placeholder="例如：数模国赛三缺一，冲国一"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={120}
            />
          </div>

          <div className="form-field">
            <label className="form-label">技能标签 <span className="required">·</span>（1~5 个，回车添加）</label>
            <div className="tag-chips">
              {tags.map((t) => (
                <span key={t} className="tag-chip">
                  {t}
                  <button type="button" className="tag-chip-rm" onClick={() => setTags(tags.filter((x) => x !== t))} aria-label={`移除 ${t}`}>×</button>
                </span>
              ))}
              <input
                className="tag-input"
                placeholder={tags.length ? '' : '如：数学建模、Python'}
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={onTagKey}
                onBlur={() => addTag(tagInput)}
              />
            </div>
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="tm-size">队伍总人数 <span className="required">·</span></label>
            <input
              id="tm-size"
              className="form-input"
              type="number"
              min={2}
              max={50}
              value={totalSize}
              onChange={(e) => setTotalSize(parseInt(e.target.value || '0', 10))}
            />
            <div className="form-hint">含你自己；招满后自动隐藏</div>
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="tm-desc">招募说明（可选）</label>
            <textarea
              id="tm-desc"
              className="form-textarea"
              placeholder="项目方向 / 时间投入 / 对队友的期待…（联系方式写在 IM 里更安全）"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={1000}
            />
            <div className="form-hint">{description.length} / 1000</div>
          </div>

          {error && <div className="form-error">{error}</div>}

          <div className="form-submit-row">
            <button
              type="submit"
              className="action-btn action-btn-primary"
              disabled={mutation.isPending}
              style={{ width: 'auto', minWidth: 200 }}
            >
              {mutation.isPending ? '发布中…' : '发布组队 →'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/app/team')}
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
