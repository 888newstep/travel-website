from pathlib import Path

root = Path(r'C:\Users\xiaohongfu\IdeaProjects\travel\frontend')

header_content = r"""import { useEffect, useMemo, useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { clearStoredAuth, getStoredToken, getStoredUsername } from '../../lib/auth'

const navItems = [
  { path: '/', label: '首页' },
  { path: '/attractions', label: '景点' },
  { path: '/routes', label: '路线' },
  { path: '/notes', label: '游记' },
  { path: '/ai-chat', label: 'AI 助手' },
  { path: '/restaurants', label: '美食' },
  { path: '/realtime', label: '实时' },
]

export function AppHeader() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [token, setToken] = useState<string | null>(() => getStoredToken())
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const syncAuth = () => setToken(getStoredToken())
    window.addEventListener('storage', syncAuth)
    window.addEventListener('auth-change', syncAuth)
    
    const handleScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', handleScroll)
    
    return () => {
      window.removeEventListener('storage', syncAuth)
      window.removeEventListener('auth-change', syncAuth)
      window.removeEventListener('scroll', handleScroll)
    }
  }, [])

  const userInitial = useMemo(() => {
    const name = getStoredUsername() || 'U'
    return name.slice(0, 1).toUpperCase()
  }, [token])

  return (
    <header className={`fixed inset-x-0 top-0 z-50 px-3 pt-3 sm:px-4 transition-all duration-300 ${scrolled ? 'backdrop-blur-xl' : ''}`}>
      <div className="app-container">
        <div className={`glass-panel rounded-2xl px-4 sm:px-5 transition-all duration-300 ${scrolled ? 'shadow-lg' : ''}`}>
          <div className="flex h-16 items-center justify-between gap-3">
            <Link to="/" className="flex min-w-0 items-center gap-3 text-slate-900 group">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-500 to-emerald-500 text-sm font-bold text-white shadow-lg shadow-sky-500/20 transition-transform group-hover:scale-110">
                T
              </span>
              <div className="min-w-0">
                <div className="truncate text-base font-semibold sm:text-lg">智慧旅游</div>
                <div className="hidden text-xs text-slate-500 sm:block">探索世界，智在掌握</div>
              </div>
            </Link>

            <nav className="hidden min-w-0 flex-1 md:flex md:justify-center">
              <div className="scrollbar-hide flex max-w-[42rem] items-center gap-1 overflow-x-auto rounded-full border border-sky-200/60 bg-white/70 px-2 py-2">
                {navItems.map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `shrink-0 rounded-full px-3 py-2 text-sm font-medium transition-all duration-200 ${
                        isActive 
                          ? 'bg-gradient-to-r from-sky-500 to-emerald-500 text-white shadow-md shadow-sky-500/20' 
                          : 'text-slate-600 hover:bg-sky-50 hover:text-sky-700'
                      }`
                    }
                  >
                    {item.label}
                  </NavLink>
                ))}
              </div>
            </nav>

            <div className="flex items-center gap-2 sm:gap-3">
              {!token ? (
                <Link 
                  to="/login" 
                  className="hidden rounded-full bg-gradient-to-r from-sky-500 to-emerald-500 px-5 py-2 text-sm font-medium text-white shadow-md shadow-sky-500/20 transition-all hover:shadow-lg hover:shadow-sky-500/30 hover:scale-105 sm:inline-flex"
                >
                  登录
                </Link>
              ) : (
                <div className="hidden items-center gap-2 sm:flex">
                  <button
                    type="button"
                    onClick={() => clearStoredAuth()}
                    className="rounded-full border border-slate-200 bg-white/90 px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-50 hover:text-slate-900"
                  >
                    退出
                  </button>
                  <Link 
                    to="/profile" 
                    className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-sky-500 to-emerald-500 text-sm font-semibold text-white shadow-lg shadow-sky-500/20 transition hover:scale-110"
                  >
                    {userInitial}
                  </Link>
                </div>
              )}

              <button
                type="button"
                className="inline-flex rounded-full border border-slate-200 bg-white/90 px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-50 md:hidden"
                onClick={() => setMobileMenuOpen((open) => !open)}
              >
                {mobileMenuOpen ? '关闭' : '菜单'}
              </button>
            </div>
          </div>

          {mobileMenuOpen ? (
            <div className="border-t border-slate-200/80 py-3 md:hidden animate-fade-in">
              <div className="grid gap-2">
                {navItems.map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    onClick={() => setMobileMenuOpen(false)}
                    className={({ isActive }) =>
                      `rounded-2xl px-4 py-3 text-sm font-medium transition-all ${
                        isActive 
                          ? 'bg-gradient-to-r from-sky-500 to-emerald-500 text-white' 
                          : 'bg-slate-50 text-slate-600 hover:bg-sky-50 hover:text-sky-700'
                      }`
                    }
                  >
                    {item.label}
                  </NavLink>
                ))}
                {token ? (
                  <button
                    type="button"
                    onClick={() => {
                      clearStoredAuth()
                      setMobileMenuOpen(false)
                    }}
                    className="rounded-2xl bg-gradient-to-r from-sky-500 to-emerald-500 px-4 py-3 text-left text-sm font-medium text-white"
                  >
                    退出登录
                  </button>
                ) : (
                  <Link
                    to="/login"
                    onClick={() => setMobileMenuOpen(false)}
                    className="rounded-2xl bg-gradient-to-r from-sky-500 to-emerald-500 px-4 py-3 text-center text-sm font-medium text-white"
                  >
                    登录
                  </Link>
                )}
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </header>
  )
}
"""

(root / 'src' / 'components' / 'layout' / 'AppHeader.tsx').write_text(header_content, encoding='utf-8', newline='\n')
print('Updated src/components/layout/AppHeader.tsx')