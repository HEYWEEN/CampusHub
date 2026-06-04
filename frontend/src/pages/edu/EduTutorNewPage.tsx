import TaskNewPage from '../tasks/TaskNewPage'

/**
 * 辅导需求发布 = task 发布 + taskType=TUTOR
 * 复用 TaskNewPage 的逻辑，但保留 /app/edu/tutor/new 路由（不重定向到 tasks，
 * 避免 URL 跳到任务发布页造成误解）。
 * 后续若需更详细的辅导专属字段（课程编号、年级、上课方式），
 * 再独立成完整页面（F-EDU-05 真实实现）。
 */
export default function EduTutorNewPage() {
  return <TaskNewPage tutor />
}
