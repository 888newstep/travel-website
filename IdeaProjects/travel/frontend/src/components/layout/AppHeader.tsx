import { useEffect, useMemo, useState } from 'react'
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
    const handleScroll = () => setScrolled(window.scrollY > 20)

    window.addEventListener('storage', syncAuth)
    window.addEventListener('auth-change', syncAuth)
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
    <header
      className={`fixed inset-x-0 top-0 z-50 px-3 pt-3 transition-all duration-300 sm:px-4 ${
        scrolled ? 'backdrop-blur-md' : ''
      }`}
    >
      <div className="app-container">
        <div
          className={`glass-panel edge-glow rounded-[1.6rem] px-4 transition-all duration-300 sm:px-5 ${
            scrolled ? 'shadow-lg shadow-sky-100/80' : ''
          }`}
        >
          <div className="flex h-16 items-center justify-between gap-3">
            <Link to="/" className="group flex min-w-0 items-center gap-3 text-slate-900">
              <span
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl
                  bg-gradient-to-br from-sky-500 to-emerald-500 text-sm font-bold text-white
                  shadow-lg shadow-sky-500/20 transition-transform group-hover:scale-110"
              >
                T
              </span>
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <div className="truncate text-base font-semibold sm:text-lg">旅行工作台</div>
                  <span className="hidden rounded-full bg-sky-50 px-2.5 py-1 text-[11px] font-medium text-sky-700 lg:inline-flex">
                    智慧旅行
                  </span>
                </div>
                <div className="hidden text-xs text-slate-500 sm:block">你的旅游灵感工作台</div>
              </div>
            </Link>

            <nav className="hidden min-w-0 flex-1 md:flex md:justify-center">
              <div className="nav-shell scrollbar-hide flex max-w-[42rem] items-center gap-1 overflow-x-auto">
                {navItems.map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `tab-pill ${isActive ? 'tab-pill-active' : 'tab-pill-idle'}`
                    }
                  >
                    {item.label}
                  </NavLink>
                ))}
              </div>
            </nav>

            <div className="flex items-center gap-2 sm:gap-3">
              {!token ? (
                <Link to="/login" className="btn-primary hidden sm:inline-flex">
                  登录
                </Link>
              ) : (
                <div className="hidden items-center gap-2 sm:flex">
                  <button type="button" onClick={() => clearStoredAuth()} className="btn-secondary">
                    退出
                  </button>
                  <Link
                    to="/profile"
                    className="flex h-11 w-11 items-center justify-center rounded-full
                      bg-gradient-to-br from-sky-500 to-emerald-500 text-sm font-semibold text-white
                      shadow-lg shadow-sky-500/20 transition hover:scale-110"
                    aria-label="个人中心"
                  >
                    {userInitial}
                  </Link>
                </div>
              )}

              <button
                type="button"
                className="btn-secondary px-3 shadow-sm md:hidden"
                onClick={() => setMobileMenuOpen((open) => !open)}
                aria-label={mobileMenuOpen ? '关闭导航菜单' : '打开导航菜单'}
              >
                {mobileMenuOpen ? '收起' : '菜单'}
              </button>
            </div>
          </div>

          {mobileMenuOpen ? (
            <div className="animate-fade-in border-t border-slate-200/80 py-3 md:hidden">
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
                    className="btn-primary w-full"
                  >
                    退出登录
                  </button>
                ) : (
                  <Link to="/login" onClick={() => setMobileMenuOpen(false)} className="btn-primary w-full">
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
