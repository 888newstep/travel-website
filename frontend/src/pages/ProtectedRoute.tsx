import type { PropsWithChildren } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useStoredToken } from '../lib/auth'

export function ProtectedRoute({ children }: PropsWithChildren) {
  const location = useLocation()
  const token = useStoredToken()

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <>{children}</>
}
