interface SearchEmptyStateProps {
  message: string
  actionLabel?: string
  onAction?: () => void
  className?: string
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
