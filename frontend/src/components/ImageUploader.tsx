import { useRef, useState } from 'react'
import { uploadImage } from '../api/upload'
import { BizError } from '../types/api'

/**
 * 通用图片上传组件 — 给所有需要"传一张图拿一个 URL"的表单复用。
 *
 * 单图模式：value: string | null, onChange(url | null)
 *   <ImageUploader value={avatarUrl} onChange={setAvatarUrl} />
 *
 * 多图模式：value: string[], onChange(urls), maxCount=N
 *   <ImageUploader value={images} onChange={setImages} multiple maxCount={5} />
 *
 * 后端约束：单文件 ≤ 5MB，仅 jpg/png/webp/gif（在 ImageStorage.put 校验）。
 */
export type ImageUploaderProps =
  | {
      multiple?: false
      value: string | null | undefined
      onChange: (url: string | null) => void
      label?: string
      hint?: string
    }
  | {
      multiple: true
      value: string[]
      onChange: (urls: string[]) => void
      maxCount?: number
      label?: string
      hint?: string
    }

export default function ImageUploader(props: ImageUploaderProps) {
  const fileRef = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string>('')

  const trigger = () => fileRef.current?.click()

  const handleFiles = async (files: FileList | null) => {
    if (!files || files.length === 0) return
    setError('')

    try {
      if (props.multiple) {
        const max = props.maxCount ?? 5
        const remain = max - props.value.length
        if (remain <= 0) {
          setError(`最多 ${max} 张`)
          return
        }
        setUploading(true)
        const chosen = Array.from(files).slice(0, remain)
        const results = await Promise.all(chosen.map((f) => uploadImage(f)))
        props.onChange([...props.value, ...results.map((r) => r.url)])
      } else {
        setUploading(true)
        const { url } = await uploadImage(files[0])
        props.onChange(url)
      }
    } catch (err) {
      setError(err instanceof BizError ? err.message : '上传失败')
    } finally {
      setUploading(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  const removeOne = (url: string) => {
    if (props.multiple) {
      props.onChange(props.value.filter((u) => u !== url))
    } else {
      props.onChange(null)
    }
  }

  const list = props.multiple ? props.value : props.value ? [props.value] : []
  const canAdd = props.multiple
    ? props.value.length < (props.maxCount ?? 5)
    : !props.value

  return (
    <div className="image-uploader">
      {props.label && <div className="image-uploader-label">{props.label}</div>}

      <div className="image-uploader-grid">
        {list.map((url) => (
          <div className="image-uploader-tile" key={url}>
            <img src={url} alt="" />
            <button
              type="button"
              className="image-uploader-remove"
              onClick={() => removeOne(url)}
              aria-label="删除"
            >
              ×
            </button>
          </div>
        ))}

        {canAdd && (
          <button
            type="button"
            className="image-uploader-add"
            onClick={trigger}
            disabled={uploading}
          >
            {uploading ? '上传中…' : (
              <>
                <span className="image-uploader-plus">+</span>
                <span className="image-uploader-add-hint">选图</span>
              </>
            )}
          </button>
        )}
      </div>

      <input
        ref={fileRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        multiple={props.multiple}
        style={{ display: 'none' }}
        onChange={(e) => handleFiles(e.target.files)}
      />

      {error && <div className="image-uploader-error">{error}</div>}
      {props.hint && !error && <div className="image-uploader-hint">{props.hint}</div>}
    </div>
  )
}
