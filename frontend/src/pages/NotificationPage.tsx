import { useEffect, useMemo, useState } from 'react'
import { notificationApi, type Notification } from '../api/notification-feedback.api'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { SearchEmptyState } from '../components/common/SearchFeedback'
import { type StatusNoticeTone, StatusNotice } from '../components/common/StatusNotice'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE } from '../constants'

function formatTime(value?: string) {
  if (!value) return '\u672a\u77e5\u65f6\u95f4'
  return value.replace('T', ' ').slice(0, 16)
}

function getTypeLabel(type: string) {
  if (type === 'system') return '\u7cfb\u7edf\u901a\u77e5'
  if (type === 'business') return '\u4e1a\u52a1\u63d0\u9192'
  if (type === 'activity') return '\u6d3b\u52a8\u6d88\u606f'
  return type || '\u901a\u77e5'
}

interface InlineNotice {
  tone: StatusNoticeTone
  message: string
}

export function NotificationPage() {
  const [loading, setLoading] = useState(true)
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loadNotice, setLoadNotice] = useState<InlineNotice | null>(null)
  const [actionNotice, setActionNotice] = useState<InlineNotice | null>(null)

  async function loadNotifications() {
    setLoading(true)
    setLoadNotice(null)
    try {
      const data = await notificationApi.getNotifications(DEFAULT_PAGE, DEFAULT_PAGE_SIZE)
      setNotifications(Array.isArray(data) ? data : [])
    } catch {
      setNotifications([])
      setLoadNotice({ tone: 'error', message: '\u901a\u77e5\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadNotifications()
  }, [])

  const unreadCount = useMemo(() => notifications.filter((item) => !item.isRead).length, [notifications])
  const readCount = notifications.length - unreadCount

  async function markRead(id?: number) {
    if (!id) return
    setActionNotice(null)
    try {
      await notificationApi.markAsRead(id)
      setNotifications((current) => current.map((item) => (item.id === id ? { ...item, isRead: true } : item)))
      setActionNotice({ tone: 'success', message: '\u6d88\u606f\u5df2\u6807\u8bb0\u4e3a\u5df2\u8bfb\u3002' })
    } catch {
      setActionNotice({ tone: 'error', message: '\u6807\u8bb0\u5df2\u8bfb\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    }
  }

  async function markAllRead() {
    if (!notifications.length || !unreadCount) return
    setActionNotice(null)
    try {
      await notificationApi.markAllAsRead()
      setNotifications((current) => current.map((item) => ({ ...item, isRead: true })))
      setActionNotice({ tone: 'success', message: '\u672a\u8bfb\u901a\u77e5\u5df2\u5168\u90e8\u6807\u8bb0\u4e3a\u5df2\u8bfb\u3002' })
    } catch {
      setActionNotice({ tone: 'error', message: '\u5168\u90e8\u6807\u8bb0\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\u3002' })
    }
  }

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

        <div className="relative grid gap-6 xl:grid-cols-[1.05fr_0.95fr] xl:items-center">
          <div>
            <span className="section-kicker">{'\u6d88\u606f\u901a\u77e5'}</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">{'\u6d88\u606f\u540c\u6b65'}</span>
              <span className="chip">{'\u5f85\u529e\u63d0\u9192'}</span>
              <span className="chip">{'\u7edf\u4e00\u6536\u4ef6\u7bb1'}</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">{'\u628a\u65c5\u884c\u76f8\u5173\u63d0\u9192\u96c6\u4e2d\u67e5\u770b'}</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              {'\u7cfb\u7edf\u901a\u77e5\u3001\u4e1a\u52a1\u63d0\u9192\u548c\u6d3b\u52a8\u6d88\u606f\u90fd\u4f1a\u6c47\u603b\u5728\u8fd9\u91cc\uff0c\u65b9\u4fbf\u4f60\u53ca\u65f6\u5904\u7406\u672a\u8bfb\u4fe1\u606f\u5e76\u8ddf\u8fdb\u884c\u7a0b\u53d8\u5316\u3002'}
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <div className="metric-card">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5f53\u524d\u9875\u6d88\u606f'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{notifications.length}</div>
              </div>
              <div className="metric-card">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5f53\u524d\u9875\u672a\u8bfb'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{unreadCount}</div>
              </div>
              <div className="metric-card">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5f53\u524d\u9875\u5df2\u8bfb'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{readCount}</div>
              </div>
            </div>
          </div>

          <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
            <div className="flex items-center justify-between gap-3">
              <div>
                <div className="text-sm font-medium text-slate-500">{'\u5904\u7406\u5efa\u8bae'}</div>
                <div className="mt-1 text-xl font-semibold text-slate-900">{'\u5148\u6e05\u672a\u8bfb\uff0c\u518d\u5904\u7406\u91cd\u70b9\u63d0\u9192'}</div>
              </div>
              <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">{'\u6536\u4ef6\u7bb1'}</span>
            </div>

            <div className="mt-4 grid gap-3 text-sm text-slate-600">
              <div className="metric-card">{'\u4f18\u5148\u5904\u7406\u672a\u8bfb\u6d88\u606f\uff0c\u907f\u514d\u9519\u8fc7\u6700\u65b0\u7684\u884c\u7a0b\u53d8\u66f4\u6216\u7cfb\u7edf\u63d0\u9192\u3002'}</div>
              <div className="metric-card">{'\u5df2\u8bfb\u6d88\u606f\u4f1a\u4fdd\u7559\u5728\u5217\u8868\u4e2d\uff0c\u65b9\u4fbf\u540e\u7eed\u518d\u6b21\u56de\u770b\u3002'}</div>
              <div className="metric-card">{'\u6d3b\u52a8\u6d88\u606f\u9002\u5408\u5feb\u901f\u6d4f\u89c8\uff0c\u4e1a\u52a1\u63d0\u9192\u5efa\u8bae\u5c3d\u5feb\u8ddf\u8fdb\u3002'}</div>
            </div>

            <div className="mt-5 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={markAllRead}
                disabled={!notifications.length || !unreadCount}
                className="btn-primary disabled:cursor-not-allowed disabled:opacity-60"
              >
                {'\u5168\u90e8\u6807\u8bb0\u5df2\u8bfb'}
              </button>
              <span className="text-sm text-slate-500">{'\u672a\u8bfb '}{unreadCount} {'\u6761'}</span>
            </div>

            {actionNotice ? <StatusNotice tone={actionNotice.tone} message={actionNotice.message} className="mt-4" /> : null}
          </div>
        </div>
      </section>

      <section className="scenic-shell-soft mt-8 p-6">
        <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <span className="section-kicker">{'\u6d88\u606f\u5217\u8868'}</span>
            <h2 className="mt-3 text-lg font-semibold text-slate-900">{'\u5f53\u524d\u9875\u63d0\u9192'}</h2>
            <p className="mt-1 text-sm text-slate-500">{'\u6309\u65f6\u95f4\u67e5\u770b\u5f53\u524d\u9875\u63d0\u9192\uff0c\u5e76\u5bf9\u672a\u8bfb\u6d88\u606f\u8fdb\u884c\u5904\u7406\u3002'}</p>
          </div>
          <span className="chip">{'\u672a\u8bfb '}{unreadCount} {'\u6761'}</span>
        </div>

        {loadNotice ? <StatusNotice tone={loadNotice.tone} message={loadNotice.message} actionLabel={'\u91cd\u8bd5'} onAction={loadNotifications} className="mb-4" /> : null}
        {loading ? <LoadingSpinner /> : null}
        {!loading && !notifications.length ? (
          <SearchEmptyState
            message={'\u6682\u65e0\u901a\u77e5\u6d88\u606f\uff0c\u7a0d\u540e\u53ef\u91cd\u65b0\u52a0\u8f7d\u67e5\u770b\u65b0\u63d0\u9192\u3002'}
            actionLabel={'\u91cd\u65b0\u52a0\u8f7d'}
            onAction={loadNotifications}
          />
        ) : null}
        {!loading && notifications.length ? (
          <div className="space-y-4">
            {notifications.map((item) => (
              <article
                key={item.id}
                className={`surface-card-hover rounded-[1.5rem] border px-5 py-4 transition ${item.isRead ? 'border-slate-200 bg-slate-50' : 'border-sky-200 bg-sky-50/80'}`}
              >
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-white/85 px-2.5 py-1 text-xs font-medium text-slate-500">
                        {getTypeLabel(item.type)}
                      </span>
                      <span className={`text-xs font-medium ${item.isRead ? 'text-slate-400' : 'text-sky-700'}`}>
                        {item.isRead ? '\u5df2\u8bfb' : '\u672a\u8bfb'}
                      </span>
                    </div>
                    <h3 className="mt-3 text-base font-semibold text-slate-900">{item.title}</h3>
                    <p className="mt-2 text-sm leading-6 text-slate-600">{item.content}</p>
                  </div>

                  <div className="flex shrink-0 flex-col items-start gap-3 sm:items-end">
                    <div className="text-xs text-slate-400">{formatTime(item.createdAt)}</div>
                    {!item.isRead ? (
                      <button type="button" onClick={() => markRead(item.id)} className="btn-secondary px-3 py-2 text-xs">
                        {'\u6807\u8bb0\u5df2\u8bfb'}
                      </button>
                    ) : null}
                  </div>
                </div>
              </article>
            ))}
          </div>
        ) : null}
      </section>
    </div>
  )
}
