import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { ErrorBoundary } from '../components/ErrorBoundary'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { HomePage } from '../pages/HomePage'
import { ProtectedRoute } from '../pages/ProtectedRoute'

const AttractionsPage = lazy(() =>
  import('../pages/AttractionsPage').then((module) => ({ default: module.AttractionsPage })),
)
const RoutesPage = lazy(() => import('../pages/RoutesPage').then((module) => ({ default: module.RoutesPage })))
const NotesPage = lazy(() => import('../pages/NotesPage').then((module) => ({ default: module.NotesPage })))
const AIChatPage = lazy(() => import('../pages/AIChatPage').then((module) => ({ default: module.AIChatPage })))
const LoginPage = lazy(() => import('../pages/LoginPage').then((module) => ({ default: module.LoginPage })))
const RestaurantPage = lazy(() =>
  import('../pages/RestaurantPage').then((module) => ({ default: module.RestaurantPage })),
)
const RealtimeStatusPage = lazy(() =>
  import('../pages/RealtimeStatusPage').then((module) => ({ default: module.RealtimeStatusPage })),
)
const UserProfilePage = lazy(() =>
  import('../pages/UserProfilePage').then((module) => ({ default: module.UserProfilePage })),
)
const NotificationPage = lazy(() =>
  import('../pages/NotificationPage').then((module) => ({ default: module.NotificationPage })),
)
const FeedbackPage = lazy(() => import('../pages/FeedbackPage').then((module) => ({ default: module.FeedbackPage })))
const FileManagementPage = lazy(() =>
  import('../pages/FileManagementPage').then((module) => ({ default: module.FileManagementPage })),
)
const RouteSharePage = lazy(() =>
  import('../pages/RouteSharePage').then((module) => ({ default: module.RouteSharePage })),
)
const RouteOptimizationPage = lazy(() =>
  import('../pages/RouteOptimizationPage').then((module) => ({ default: module.RouteOptimizationPage })),
)

function RouteFallback() {
  return (
    <div className="app-container min-h-[50vh]">
      <LoadingSpinner />
    </div>
  )
}

export function AppRouter() {
  return (
    <ErrorBoundary>
      <Suspense fallback={<RouteFallback />}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/attractions" element={<AttractionsPage />} />
          <Route path="/routes" element={<RoutesPage />} />
          <Route path="/notes" element={<NotesPage />} />
          <Route path="/ai-chat" element={<AIChatPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/restaurants" element={<RestaurantPage />} />
          <Route path="/realtime" element={<RealtimeStatusPage />} />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <UserProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notifications"
            element={
              <ProtectedRoute>
                <NotificationPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/feedback"
            element={
              <ProtectedRoute>
                <FeedbackPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/files"
            element={
              <ProtectedRoute>
                <FileManagementPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/share"
            element={
              <ProtectedRoute>
                <RouteSharePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/optimization"
            element={
              <ProtectedRoute>
                <RouteOptimizationPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </ErrorBoundary>
  )
}
