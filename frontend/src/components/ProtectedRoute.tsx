import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../stores/auth'

interface Props {
  children: ReactNode
  /** 是否额外要求已通过学生认证（默认否） */
  requireVerified?: boolean
}

export default function ProtectedRoute({ children, requireVerified = false }: Props) {
  const accessToken = useAuthStore((s) => s.accessToken)
  const verifyStatus = useAuthStore((s) => s.verifyStatus)
  const location = useLocation()

  if (!accessToken) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  if (requireVerified && verifyStatus !== 'approved') {
    return <Navigate to="/verify" state={{ from: location.pathname }} replace />
  }
  return <>{children}</>
}
