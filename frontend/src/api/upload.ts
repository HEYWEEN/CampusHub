import { apiPost } from './client'

/**
 * 通用图片上传 — 对应后端 POST /api/uploads。
 *
 * 流程：
 *   File / Blob → FormData (字段名 "file") → multipart 上传 → 拿 {url}
 *
 * 用法：业务表单先调本函数拿到 url，再把 url 塞进业务接口的 JSON 字段
 *   const { url } = await uploadImage(file)
 *   await updateProfile({ avatarUrl: url })
 *
 * 后端校验：
 *   - 单文件 ≤ 5MB
 *   - MIME 只接 jpg/png/webp/gif
 */
export interface UploadResultVO {
  url: string
}

export async function uploadImage(file: File): Promise<UploadResultVO> {
  const fd = new FormData()
  fd.append('file', file)
  // axios 在 body 是 FormData 时会自动改 Content-Type 为 multipart/form-data
  // 并补上 boundary；显式传 undefined 让 axios 自己处理（避免被全局 JSON 头覆盖）
  return apiPost<UploadResultVO>('/api/uploads', fd as unknown as object, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
