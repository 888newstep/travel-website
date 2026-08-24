import type { PropsWithChildren } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

export function ProtectedRoute({ children }: PropsWithChildren) {
  const location = useLocation()
  const token = localStorage.getItem('token')

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <>{children}</>
}
