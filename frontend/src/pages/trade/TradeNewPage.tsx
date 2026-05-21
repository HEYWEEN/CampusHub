import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createItem } from '../../api/trade'
import type { PickupType, TradeItemCreateDTO } from '../../types/trade'
import { BizError } from '../../types/api'
import '../tasks/Tasks.css'
import './Trade.css'

const PICKUP_OPTIONS: { value: PickupType; label: string; hint: string }[] = [
  { value: 'EXACT_DORM',     label: '精确宿舍', hint: '只对宿舍号匹配的同学公开' },
  { value: 'BUILDING_RANGE', label: '楼栋范围', hint: '在指定的楼栋范围内可见（默认推荐）' },
  { value: 'MEETING',        label: '面交',     hint: '任意校内地点见面交易' },
]

// 演示用：从已有 illustrations 里选一个作占位"图片"
const DEMO_IMAGES = [
  '/illustrations/coffee.png',
  '/illustrations/reading.png',
  '/illustrations/free-time.png',
  '/illustrations/focused.png',
  '/illustrations/reflecting.png',
  '/illustrations/catching-up.png',
]

export default function TradeNewPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()

  const [title, setTitle] = useState('')
  const [price, setPrice] = useState(50)
  const [description, setDescription] = useState('')
  const [pickupType, setPickupType] = useState<PickupType>('BUILDING_RANGE')
  const [buildingRange, setBuildingRange] = useState('')
  const [pickedImage, setPickedImage] = useState<string>(DEMO_IMAGES[0])
  const [error, setError] = useState('')

  const mutation = useMutation({
    mutationFn: (dto: TradeItemCreateDTO) => createItem(dto),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['items'] })
      navigate(`/app/trade/${data.itemId}`, { replace: true })
    },
    onError: (err) => setError(err instanceof BizError ? err.message : '发布失败'),
  })

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (!title.trim() || title.length > 50) { setError('标题需 1-50 字'); return }
    if (!description.trim() || description.length > 500) { setError('描述需 1-500 字'); return }
    if (price < 0 || price > 100000) { setError('价格 0-100000 积分'); return }
    mutation.mutate({
      title: title.trim(),
      price,
      description: description.trim(),
      images: [pickedImage],
      pickupType,
      buildingRange: buildingRange.trim() || undefined,
    })
  }

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">
          挂个<span className="it">出售</span>。
        </h1>
        <div className="page-sub">F-TRADE-01 · 9 图 EXIF 清洗（演示版用预设图）</div>
      </div>

      <form className="task-form" onSubmit={handleSubmit} noValidate>
        <div className="form-field">
          <label className="form-label">商品图 · 演示用（实际部署后支持上传）</label>
          <div className="image-uploader">
            {DEMO_IMAGES.map((img) => (
              <button
                type="button"
                key={img}
                onClick={() => setPickedImage(img)}
                className={`image-slot has-image${pickedImage === img ? '' : ''}`}
                style={{
                  borderColor: pickedImage === img ? 'var(--accent)' : undefined,
                  borderWidth: pickedImage === img ? '2px' : undefined,
                }}
              >
                <img src={img} alt="" />
              </button>
            ))}
          </div>
        </div>

        <div className="form-field">
          <label className="form-label" htmlFor="t-title">
            标题 <span className="required">·</span>
          </label>
          <input
            id="t-title"
            className="form-input"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={50}
            placeholder="例如：Kindle Paperwhite 4（无划痕）"
          />
          <div className="form-hint">{title.length} / 50</div>
        </div>

        <div className="form-row">
          <div className="form-field">
            <label className="form-label" htmlFor="t-price">
              售价（积分） <span className="required">·</span>
            </label>
            <input
              id="t-price"
              className="form-input"
              type="number"
              min={0}
              max={100000}
              value={price}
              onChange={(e) => setPrice(parseInt(e.target.value || '0', 10))}
            />
          </div>
          <div className="form-field">
            <label className="form-label">取货方式</label>
            <select
              className="form-input"
              value={pickupType}
              onChange={(e) => setPickupType(e.target.value as PickupType)}
            >
              {PICKUP_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
            <div className="form-hint">
              {PICKUP_OPTIONS.find((o) => o.value === pickupType)?.hint}
            </div>
          </div>
        </div>

        <div className="form-field">
          <label className="form-label" htmlFor="t-range">具体位置（可选）</label>
          <input
            id="t-range"
            className="form-input"
            value={buildingRange}
            onChange={(e) => setBuildingRange(e.target.value)}
            maxLength={50}
            placeholder="例如：仙林 14 号楼 / 鼓楼校区"
          />
        </div>

        <div className="form-field">
          <label className="form-label" htmlFor="t-desc">
            描述 <span className="required">·</span>
          </label>
          <textarea
            id="t-desc"
            className="form-textarea"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={500}
            placeholder="新旧程度 / 购入时间 / 是否包邮 / 联系方式（IM 更安全）"
          />
          <div className="form-hint">{description.length} / 500</div>
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
  )
}
