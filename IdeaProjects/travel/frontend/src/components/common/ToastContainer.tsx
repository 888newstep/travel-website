import { useEffect, useState } from 'react'
import { subscribeToToasts, removeToast, type Toast } from '../../lib/toast'

const typeStyles = {
  info: 'border-sky-200 bg-sky-50 text-sky-800',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  warning: 'border-amber-200 bg-amber-50 text-amber-800',
  error: 'border-rose-200 bg-rose-50 text-rose-800',
}

const typeIcons = {
  info: 'ℹ️',
  success: '✅',
  warning: '⚠️',
  error: '⛔',
}

export function ToastContainer() {
  const [toasts, setToasts] = useState<Toast[]>([])

  useEffect(() => subscribeToToasts(setToasts), [])

  if (!toasts.length) {
    return null
  }

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <div key={toast.id} className={`animate-slide-up rounded-2xl border px-4 py-3 text-sm shadow-lg ${typeStyles[toast.type]}`}>
          <div className="flex items-start gap-3">
            <span className="text-lg">{typeIcons[toast.type]}</span>
            <div className="flex-1">{toast.message}</div>
            <button type="button" onClick={() => removeToast(toast.id)} className="text-xs opacity-60 hover:opacity-100">
              关闭
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}
