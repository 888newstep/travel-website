import { Link } from 'react-router-dom'

const footerLinks = [
  { label: '景点推荐', to: '/attractions' },
  { label: '路线规划', to: '/routes' },
  { label: 'AI 助手', to: '/ai-chat' },
  { label: '旅行游记', to: '/notes' },
]

export function AppFooter() {
  return (
    <footer className="relative z-10 mt-14 overflow-hidden border-t border-sky-200/60 bg-gradient-to-br from-white/80 via-sky-50/70 to-emerald-50/70 backdrop-blur-sm">
      <div className="footer-ribbon" />
      <div className="scenic-orb scenic-orb-sky -left-8 top-6 h-28 w-28 opacity-50" />
      <div className="scenic-orb scenic-orb-emerald right-8 top-8 h-32 w-32 opacity-45" />

      <div className="app-container relative py-8">
        <div className="grid gap-8 md:grid-cols-[1.3fr_0.7fr] md:items-start">
          <div>
            <div className="flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-500 to-emerald-500 text-sm font-bold text-white shadow-lg shadow-sky-500/20">
                T
              </span>
              <div className="text-lg font-semibold text-slate-900">旅行工作台</div>
            </div>
            <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-500">
              把景点、路线、实时信息和 AI 能力整理在同一处，帮助你更轻松地完成旅行规划、调整和记录。
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              <span className="chip">景点</span>
              <span className="chip">路线</span>
              <span className="chip">AI 助手</span>
            </div>
          </div>

          <div>
            <div className="text-sm font-medium text-slate-900">快速入口</div>
            <div className="mt-3 grid gap-2 sm:grid-cols-2">
              {footerLinks.map((item) => (
                <Link
                  key={item.to}
                  to={item.to}
                  className="rounded-2xl border border-white/80 bg-white/70 px-4 py-3 text-sm text-slate-500 transition hover:border-sky-200 hover:text-sky-600"
                >
                  {item.label}
                </Link>
              ))}
            </div>
          </div>
        </div>

        <div className="mt-8 border-t border-slate-200/60 pt-6 text-center text-xs text-slate-400">
          © 2026 旅行工作台 · 基于 React + TypeScript 构建
        </div>
      </div>
    </footer>
  )
}
