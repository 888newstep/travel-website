from pathlib import Path

root = Path(r'C:\Users\xiaohongfu\IdeaProjects\travel\frontend')

footer_content = r"""import { Link } from 'react-router-dom'

const footerLinks = [
  { label: '景点探索', to: '/attractions' },
  { label: '路线规划', to: '/routes' },
  { label: 'AI 助手', to: '/ai-chat' },
  { label: '游记笔记', to: '/notes' },
]

export function AppFooter() {
  return (
    <footer className="relative z-10 mt-12 border-t border-sky-200/60 bg-white/70 backdrop-blur-sm">
      <div className="app-container py-8">
        <div className="grid gap-8 md:grid-cols-[1.3fr_0.7fr] md:items-start">
          <div>
            <div className="flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-500 to-emerald-500 text-sm font-bold text-white shadow-lg shadow-sky-500/20">
                T
              </span>
              <div className="text-lg font-semibold text-slate-900">智慧旅游</div>
            </div>
            <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-500">
              探索世界，智在掌握。为您提供智能景点推荐、路线规划、实时动态等一站式旅游服务。
            </p>
          </div>
          <div>
            <div className="text-sm font-medium text-slate-900">快速入口</div>
            <div className="mt-3 grid gap-2">
              {footerLinks.map((item) => (
                <Link 
                  key={item.to} 
                  to={item.to} 
                  className="text-sm text-slate-500 transition hover:text-sky-600"
                >
                  {item.label}
                </Link>
              ))}
            </div>
          </div>
        </div>
        <div className="mt-8 border-t border-slate-200/60 pt-6 text-center text-xs text-slate-400">
          © 2024 智慧旅游系统 · 基于 React + TypeScript 构建
        </div>
      </div>
    </footer>
  )
}
"""

(root / 'src' / 'components' / 'layout' / 'AppFooter.tsx').write_text(footer_content, encoding='utf-8', newline='\n')
print('Updated src/components/layout/AppFooter.tsx')