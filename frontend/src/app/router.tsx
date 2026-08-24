import { Navigate, Route, Routes } from 'react-router-dom'
import { AIChatPage } from '../pages/AIChatPage'
import { AttractionsPage } from '../pages/AttractionsPage'
import { FeedbackPage } from '../pages/FeedbackPage'
import { FileManagementPage } from '../pages/FileManagementPage'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { NotesPage } from '../pages/NotesPage'
import { NotificationPage } from '../pages/NotificationPage'
import { ProtectedRoute } from '../pages/ProtectedRoute'
import { RealtimeStatusPage } from '../pages/RealtimeStatusPage'
import { RestaurantPage } from '../pages/RestaurantPage'
import { RouteOptimizationPage } from '../pages/RouteOptimizationPage'
import { RouteSharePage } from '../pages/RouteSharePage'
import { RoutesPage } from '../pages/RoutesPage'
import { UserProfilePage } from '../pages/UserProfilePage'

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/attractions" element={<AttractionsPage />} />
      <Route path="/routes" element={<RoutesPage />} />
      <Route path="/notes" element={<NotesPage />} />
      <Route path="/ai-chat" element={<AIChatPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/restaurants" element={<RestaurantPage />} />
      <Route path="/realtime" element={<RealtimeStatusPage />} />
      <Route path="/profile" element={<ProtectedRoute><UserProfilePage /></ProtectedRoute>} />
      <Route path="/notifications" element={<ProtectedRoute><NotificationPage /></ProtectedRoute>} />
      <Route path="/feedback" element={<ProtectedRoute><FeedbackPage /></ProtectedRoute>} />
      <Route path="/files" element={<ProtectedRoute><FileManagementPage /></ProtectedRoute>} />
      <Route path="/share" element={<ProtectedRoute><RouteSharePage /></ProtectedRoute>} />
      <Route path="/optimization" element={<ProtectedRoute><RouteOptimizationPage /></ProtectedRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
