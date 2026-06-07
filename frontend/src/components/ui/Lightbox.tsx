import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import './Dialog.css'

/**
 * 图片灯箱：点击缩略图后全屏看原图（bug 13）。
 * src 为 null 即关闭；点遮罩 / × / ESC 关闭。
 */
export default function Lightbox({ src, onClose }: { src: string | null; onClose: () => void }) {
  useEffect(() => {
    if (!src) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [src, onClose])

  if (!src) return null

  return createPortal(
    <div className="lightbox-overlay" onClick={onClose}>
      <button type="button" className="lightbox-close" onClick={onClose} aria-label="关闭">×</button>
      <img className="lightbox-img" src={src} alt="查看大图" onClick={(e) => e.stopPropagation()} />
    </div>,
    document.body,
  )
}
