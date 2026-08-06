import { useEffect, useMemo, useState } from 'react'
import { notificationApi, type Notification } from '../api/notification-feedback.api'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE } from '../constants'

function formatTime(value?: string) {
  if (!value) return '未知时间'
  return value.replace('T', ' ').slice(0, 16)
}

function getTypeLabel(type: string) {
  if (type === 'system') return '系统通知'
  if (type === 'business') return '业务提醒'
  if (type === 'activity') return '活动消息'
  return type || '通知'
}

export function NotificationPage() {
  const [loading, setLoading] = useState(true)
  const [notifications, setNotifications] = useState<Notification[]>([])

  useEffect(() => {
    let active = true

    notificationApi
      .getNotifications(DEFAULT_PAGE, DEFAULT_PAGE_SIZE)
      .then((data) => {
        if (active) {
          setNotifications(Array.isArray(data) ? data : [])
        }
      })
      .catch(() => {
        if (active) setNotifications([])
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const unreadCount = useMemo(() => notifications.filter((item) => !item.isRead).length, [notifications])
  const readCount = notifications.length - unreadCount

  async function markRead(id?: number) {
    if (!id) return
    try {
      await notificationApi.markAsRead(id)
      setNotifications((current) => current.map((item) => (item.id === id ? { ...item, isRead: true } : item)))
    } catch {
      // ignore
    }
  }

  async function markAllRead() {
    try {
      await notificationApi.markAllAsRead()
      setNotifications((current) => current.map((item) => ({ ...item, isRead: true })))
    } catch {
      // ignore
    }
  }

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

        <div className="relative grid gap-6 xl:grid-cols-[1.05fr_0.95fr] xl:items-center">
          <div>
            <span className="section-kicker">消息通知</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">消息同步</span>
              <span className="chip">待办提醒</span>
              <span className="chip">统一收件箱</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">把旅行相关提醒集中查看</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              系统通知、业务提醒和活动消息都会汇总在这里，方便你及时处理未读信息并跟进行程变化。
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <div className="metric-card">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">消息总数</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{notifications.length}</div>
              </div>
              <div className="metric-card">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">未读消息</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{unreadCount}</div>
              </div>
              <div className="metric-card">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">已读消息</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{readCount}</div>
              </div>
            </div>
          </div>

          <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
            <div className="flex items-center justify-between gap-3">
              <div>
                <div className="text-sm font-medium text-slate-500">处理建议</div>
                <div className="mt-1 text-xl font-semibold text-slate-900">先清未读，再处理重点提醒</div>
              </div>
              <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">收件箱</span>
            </div>

            <div className="mt-4 grid gap-3 text-sm text-slate-600">
              <div className="metric-card">优先处理未读消息，避免错过最新的行程变更或系统提醒。</div>
              <div className="metric-card">已读消息会保留在列表中，方便后续再次回看。</div>
              <div className="metric-card">活动消息适合快速浏览，业务提醒建议尽快跟进。</div>
            </div>

            <div className="mt-5 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={markAllRead}
                disabled={!notifications.length || !unreadCount}
                className="btn-primary disabled:cursor-not-allowed disabled:opacity-60"
              >
                全部标记已读
              </button>
              <span className="text-sm text-slate-500">未读 {unreadCount} 条</span>
            </div>
          </div>
        </div>
      </section>

      <section className="scenic-shell-soft mt-8 p-6">
        <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <span className="section-kicker">消息列表</span>
            <h2 className="mt-3 text-lg font-semibold text-slate-900">全部提醒</h2>
            <p className="mt-1 text-sm text-slate-500">按时间查看全部提醒，并对未读消息进行处理。</p>
          </div>
          <span className="chip">未读 {unreadCount} 条</span>
        </div>

        {loading ? <LoadingSpinner /> : null}
        {!loading && !notifications.length ? <div className="py-10 text-center text-sm text-slate-400">暂无通知消息</div> : null}
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
                        {item.isRead ? '已读' : '未读'}
                      </span>
                    </div>
                    <h3 className="mt-3 text-base font-semibold text-slate-900">{item.title}</h3>
                    <p className="mt-2 text-sm leading-6 text-slate-600">{item.content}</p>
                  </div>

                  <div className="flex shrink-0 flex-col items-start gap-3 sm:items-end">
                    <div className="text-xs text-slate-400">{formatTime(item.createdAt)}</div>
                    {!item.isRead ? (
                      <button type="button" onClick={() => markRead(item.id)} className="btn-secondary px-3 py-2 text-xs">
                        标记已读
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
