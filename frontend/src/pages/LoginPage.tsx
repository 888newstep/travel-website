import { useMemo, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { userApi } from '../api/user.api'
import { notifyAuthChanged, persistAuth } from '../lib/auth'

type Mode = 'login' | 'register'

interface FormState {
  username: string
  phone: string
  password: string
  captcha: string
}

const initialForm: FormState = {
  username: '',
  phone: '',
  password: '',
  captcha: '',
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const redirectTo = useMemo(() => {
    const state = location.state as { from?: string } | null
    return state?.from || '/'
  }, [location.state])

  const [mode, setMode] = useState<Mode>('login')
  const [form, setForm] = useState<FormState>(initialForm)
  const [loading, setLoading] = useState(false)
  const [sendingCaptcha, setSendingCaptcha] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  function updateField<K extends keyof FormState>(field: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function handleSendCaptcha() {
    if (!form.phone.trim()) {
      setError('请先输入手机号')
      return
    }

    setSendingCaptcha(true)
    setError('')
    setMessage('')

    try {
      const response = await userApi.sendCaptcha(form.phone.trim())
      updateField('captcha', response.demoCode)
      setMessage(`本地演示验证码：${response.demoCode}`)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : '验证码发送失败，请稍后重试')
    } finally {
      setSendingCaptcha(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setMessage('')

    try {
      if (mode === 'login') {
        const response = await userApi.login({
          username: form.phone.trim(),
          password: form.password,
        })

        const stored = persistAuth(response)
        if (!stored) {
          localStorage.setItem('username', form.phone.trim())
          notifyAuthChanged()
        }

        navigate(redirectTo, { replace: true })
        return
      }

      await userApi.register({
        username: form.username.trim(),
        phone: form.phone.trim(),
        password: form.password,
        captcha: form.captcha.trim(),
        agreement: true,
      })

      setMode('login')
      setMessage('注册成功，请使用新账号登录')
      setForm((current) => ({
        ...current,
        password: '',
        captcha: '',
      }))
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : '提交失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="px-4 py-10 sm:px-6 lg:px-8">
      <div className="mx-auto grid min-h-[calc(100vh-7rem)] w-full max-w-6xl gap-8 lg:grid-cols-[1.05fr_0.95fr] lg:items-center">
        <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
          <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
          <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

          <div className="relative">
            <Link to="/" className="inline-flex items-center gap-3 text-slate-900">
              <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-500 to-emerald-500 text-sm font-bold text-white shadow-lg shadow-sky-500/20">
                T
              </span>
              <span className="text-lg font-semibold">{'\u65c5\u884c\u5de5\u4f5c\u53f0'}</span>
            </Link>

            <span className="section-kicker mt-6 inline-flex">{'\u8d26\u53f7\u5165\u53e3'}</span>

            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">安全登录</span>
              <span className="chip">手机号注册</span>
              <span className="chip">AI 行程同步</span>
            </div>

            <h1 className="mt-6 max-w-2xl text-4xl font-semibold tracking-tight text-slate-900 md:text-5xl">
              {mode === 'login' ? '登录后继续你的旅行规划' : '创建账号，保存你的旅行灵感'}
            </h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              登录后可同步路线、收藏、游记和 AI 对话记录，让你的旅行计划在不同页面之间自然衔接。
            </p>

            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <div className="metric-card">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">同步内容</div>
                <div className="mt-2 text-base font-semibold text-slate-900">路线与收藏</div>
              </div>
              <div className="metric-card">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">账号体验</div>
                <div className="mt-2 text-base font-semibold text-slate-900">安全、轻量、易上手</div>
              </div>
              <div className="metric-card">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">AI 能力</div>
                <div className="mt-2 text-base font-semibold text-slate-900">继续使用智能旅行能力</div>
              </div>
            </div>
          </div>
        </section>

        <section className="scenic-shell-soft p-6 sm:p-8">
          <div className="mb-6 text-center lg:text-left">
            <span className="section-kicker">登录</span>
            <h2 className="mt-3 text-2xl font-semibold text-slate-900">{mode === 'login' ? '账号登录' : '创建账号'}</h2>
            <p className="mt-2 text-sm text-slate-500">
              {mode === 'login' ? '登录后可同步你的路线、收藏和游记内容。' : '完成注册后即可开始保存路线、笔记和个人偏好。'}
            </p>
          </div>

          <div className="mb-6 rounded-full border border-sky-200/80 bg-sky-50/90 p-1.5">
            <div className="grid grid-cols-2 gap-1">
              <button
                type="button"
                className={`tab-pill ${mode === 'login' ? 'tab-pill-active' : 'tab-pill-idle'}`}
                onClick={() => {
                  setMode('login')
                  setError('')
                  setMessage('')
                }}
              >
                登录
              </button>
              <button
                type="button"
                className={`tab-pill ${mode === 'register' ? 'tab-pill-active' : 'tab-pill-idle'}`}
                onClick={() => {
                  setMode('register')
                  setError('')
                  setMessage('')
                }}
              >
                注册
              </button>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {mode === 'register' ? (
              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-500">用户名</label>
                <input
                  value={form.username}
                  onChange={(event) => updateField('username', event.target.value)}
                  type="text"
                  required
                  placeholder="请输入用户名"
                  className="search-input"
                />
              </div>
            ) : null}

            <div>
              <label className="mb-1.5 block text-xs font-medium text-slate-500">{mode === 'login' ? '手机号 / 用户名' : '手机号'}</label>
              <input
                value={form.phone}
                onChange={(event) => updateField('phone', event.target.value)}
                type={mode === 'login' ? 'text' : 'tel'}
                required
                placeholder={mode === 'login' ? '请输入手机号或用户名' : '请输入手机号'}
                className="search-input"
              />
            </div>

            <div>
              <label className="mb-1.5 block text-xs font-medium text-slate-500">密码</label>
              <input
                value={form.password}
                onChange={(event) => updateField('password', event.target.value)}
                type="password"
                required
                placeholder="请输入密码"
                className="search-input"
              />
            </div>

            {mode === 'register' ? (
              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-500">验证码</label>
                <div className="flex gap-3">
                  <input
                    value={form.captcha}
                    onChange={(event) => updateField('captcha', event.target.value)}
                    type="text"
                    required
                    placeholder="请输入短信验证码"
                    className="search-input min-w-0 flex-1"
                  />
                  <button type="button" onClick={handleSendCaptcha} disabled={sendingCaptcha} className="btn-secondary shrink-0 px-4 py-3 disabled:cursor-not-allowed disabled:opacity-60">
                    {sendingCaptcha ? '发送中...' : '发送验证码'}
                  </button>
                </div>
              </div>
            ) : null}

            {error ? <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div> : null}
            {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

            <button type="submit" disabled={loading} className="btn-primary w-full disabled:cursor-not-allowed disabled:opacity-60">
              {loading ? '提交中...' : mode === 'login' ? '立即登录' : '完成注册'}
            </button>
          </form>
        </section>
      </div>
    </div>
  )
}
