import { AppHeader } from '../components/layout/AppHeader'
import { AppFooter } from '../components/layout/AppFooter'
import { ToastContainer } from '../components/common/ToastContainer'
import { AppRouter } from './router'

export function App() {
  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-sky-50 via-white to-emerald-50 text-slate-900">
      <div
        className="pointer-events-none fixed inset-x-0 top-0 z-0 h-72
          bg-[radial-gradient(circle_at_top_right,_rgba(14,165,233,0.15),_transparent_30%),radial-gradient(circle_at_top_left,_rgba(16,185,129,0.12),_transparent_32%)]"
      />
      <AppHeader />
      <main className="relative z-10 flex-1 pt-20 md:pt-24">
        <AppRouter />
      </main>
      <AppFooter />
      <ToastContainer />
    </div>
  )
}
