interface SearchSyncNoticeProps {
  searching: boolean
  syncingText?: string
  syncedText?: string
  className?: string
}

interface SearchEmptyStateProps {
  message: string
  actionLabel?: string
  onAction?: () => void
  className?: string
}

export function SearchSyncNotice({
  searching,
  syncingText = '\u6b63\u5728\u540c\u6b65\u641c\u7d22\u7ed3\u679c...',
  syncedText = '\u641c\u7d22\u7ed3\u679c\u5df2\u4e0e\u5f53\u524d\u5173\u952e\u8bcd\u540c\u6b65\u3002',
  className = '',
}: SearchSyncNoticeProps) {
  return (
    <div className={`rounded-2xl bg-sky-50 px-4 py-3 text-sky-700 ${className}`.trim()}>
      {searching ? syncingText : syncedText}
    </div>
  )
}

export function SearchEmptyState({ message, actionLabel, onAction, className = '' }: SearchEmptyStateProps) {
  return (
    <div className={`surface-card rounded-[1.75rem] p-10 text-center text-sm text-slate-500 ${className}`.trim()}>
      <div>{message}</div>
      {actionLabel && onAction ? (
        <button type="button" onClick={onAction} className="btn-secondary mt-4 min-w-[7rem]">
          {actionLabel}
        </button>
      ) : null}
    </div>
  )
}
