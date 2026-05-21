import { Navigate } from 'react-router-dom'

/**
 * 辅导需求发布 = task 发布 + taskType=TUTOR
 * 复用 TaskNewPage 即可，这里只是路由别名。
 * 后续若需更详细的辅导专属字段（课程编号、年级、上课方式），
 * 再独立成完整页面（F-EDU-05 真实实现）。
 */
export default function EduTutorNewPage() {
  return <Navigate to="/app/tasks/new?type=TUTOR" replace />
}
