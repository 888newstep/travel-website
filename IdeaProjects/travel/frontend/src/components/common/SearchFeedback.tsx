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
  syncingText = 'MessageListMessageList...',
  syncedText = 'MessageListMessageListMessageList??',
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
