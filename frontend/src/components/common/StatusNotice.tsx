import type { ReactNode } from 'react'

export type StatusNoticeTone = 'info' | 'success' | 'warning' | 'error'

interface StatusNoticeProps {
  tone?: StatusNoticeTone
  title?: string
  message: ReactNode
  actionLabel?: string
  onAction?: () => void
  className?: string
}

const toneClassMap: Record<StatusNoticeTone, string> = {
  info: 'border-sky-200 bg-sky-50/90 text-sky-800',
  success: 'border-emerald-200 bg-emerald-50/90 text-emerald-800',
  warning: 'border-amber-200 bg-amber-50/90 text-amber-800',
  error: 'border-rose-200 bg-rose-50/90 text-rose-800',
}

const toneTitleMap: Record<StatusNoticeTone, string> = {
  info: '\u72b6\u6001\u63d0\u793a',
  success: '\u64cd\u4f5c\u5b8c\u6210',
  warning: '\u8bf7\u7559\u610f',
  error: '\u5904\u7406\u5931\u8d25',
}

export function StatusNotice({
  tone = 'info',
  title,
  message,
  actionLabel,
  onAction,
  className = '',
}: StatusNoticeProps) {
  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={`rounded-2xl border px-4 py-3 text-sm shadow-sm ${toneClassMap[tone]} ${className}`.trim()}
    >
      <div className="font-medium">{title || toneTitleMap[tone]}</div>
      <div className="mt-1 leading-6">{message}</div>
      {actionLabel && onAction ? (
        <button type="button" onClick={onAction} className="btn-secondary mt-3 min-w-[6rem]">
          {actionLabel}
        </button>
      ) : null}
    </div>
  )
}
