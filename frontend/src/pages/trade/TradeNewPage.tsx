import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createItem } from '../../api/trade'
import type { PickupLocationType, TradeItemCreateDTO } from '../../types/trade'
import { BizError } from '../../types/api'
import ImageUploader from '../../components/ImageUploader'
import Select from '../../components/Select'
import '../tasks/Tasks.css'
import './Trade.css'

const PICKUP_OPTIONS: { value: PickupLocationType; label: string; hint: string }[] = [
  { value: 'EXACT_DORM',     label: '精确宿舍', hint: '只对宿舍号匹配的同学公开' },
  { value: 'BUILDING_RANGE', label: '楼栋范围', hint: '在指定的楼栋范围内可见（默认推荐）' },
  { value: 'MEETUP',         label: '面交',     hint: '任意校内地点见面交易' },
]

export default function TradeNewPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()

  const [title, setTitle] = useState('')
  const [pricePoint, setPricePoint] = useState(50)
  const [description, setDescription] = useState('')
  const [pickupLocationType, setPickupLocationType] = useState<PickupLocationType>('BUILDING_RANGE')
  const [pickupLocationDetail, setPickupLocationDetail] = useState('')
  const [imageUrls, setImageUrls] = useState<string[]>([])
  const [error, setError] = useState('')

  const mutation = useMutation({
    mutationFn: (dto: TradeItemCreateDTO) => createItem(dto),
    onSuccess: (item) => {
      qc.invalidateQueries({ queryKey: ['items'] })
      navigate(`/app/trade/${item.id}`, { replace: true })
    },
    onError: (err) => setError(err instanceof BizError ? err.message : '发布失败'),
  })

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (!title.trim() || title.length > 200) { setError('标题需 1-200 字'); return }
    if (description && description.length > 2000) { setError('描述最多 2000 字'); return }
    if (pricePoint < 1 || pricePoint > 100000) { setError('价格 1-100000 积分'); return }
    if (imageUrls.length === 0) { setError('请至少上传 1 张商品图'); return }
    mutation.mutate({
      title: title.trim(),
      pricePoint,
      description: description.trim() || undefined,
      imageUrls,
      pickupLocationType,
      pickupLocationDetail: pickupLocationDetail.trim() || undefined,
    })
  }

  return (
    <div className="wrap">
      <div className="form-page">
        <div className="form-page-head">
          <h1 className="page-title">
            挂个<span className="it">出售</span>
          </h1>
          <p className="form-page-sub">拍清楚、写明白，闲置更快出手。</p>
        </div>

        <form className="task-form form-card" onSubmit={handleSubmit} noValidate>
          <div className="form-field">
            <label className="form-label">商品图 <span className="required">·</span></label>
            <ImageUploader
              multiple
              maxCount={9}
              value={imageUrls}
              onChange={setImageUrls}
              hint="jpg/png/webp/gif，单张 ≤ 5MB，最多 9 张"
            />
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="t-title">标题 <span className="required">·</span></label>
            <input
              id="t-title"
              className="form-input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={200}
              placeholder="例如：Kindle Paperwhite 4（无划痕）"
            />
            <div className="form-hint">{title.length} / 200</div>
          </div>

          <div className="form-row">
            <div className="form-field">
              <label className="form-label" htmlFor="t-price">售价（积分） <span className="required">·</span></label>
              <input
                id="t-price"
                className="form-input"
                type="number"
                min={1}
                max={100000}
                value={pricePoint}
                onChange={(e) => setPricePoint(parseInt(e.target.value || '0', 10))}
              />
            </div>
            <div className="form-field">
              <label className="form-label">取货方式</label>
              <Select
                value={pickupLocationType}
                options={PICKUP_OPTIONS}
                onChange={setPickupLocationType}
                ariaLabel="取货方式"
              />
              <div className="form-hint">
                {PICKUP_OPTIONS.find((o) => o.value === pickupLocationType)?.hint}
              </div>
            </div>
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="t-detail">具体位置（可选）</label>
            <input
              id="t-detail"
              className="form-input"
              value={pickupLocationDetail}
              onChange={(e) => setPickupLocationDetail(e.target.value)}
              maxLength={200}
              placeholder="例如：仙林 14 号楼 / 鼓楼校区"
            />
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="t-desc">描述（可选）</label>
            <textarea
              id="t-desc"
              className="form-textarea"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={2000}
              placeholder="新旧程度 / 购入时间 / 是否包邮 / 联系方式（IM 更安全）"
            />
            <div className="form-hint">{description.length} / 2000</div>
          </div>

          {error && <div className="form-error">{error}</div>}

          <div className="form-submit-row">
            <button
              type="submit"
              className="action-btn action-btn-primary"
              disabled={mutation.isPending}
              style={{ width: 'auto', minWidth: 200 }}
            >
              {mutation.isPending ? '发布中…' : '挂个出售 →'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/app/trade')}
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
