interface EmptyStateProps {
  icon?: string
  title: string
  description?: string
  actionLabel?: string
  onAction?: () => void
}

export function EmptyState({ icon = '🌤️', title, description, actionLabel, onAction }: EmptyStateProps) {
  return (
    <div className="scenic-shell-soft p-10 text-center">
      <div className="mb-4 text-5xl">{icon}</div>
      <h3 className="mb-2 text-lg font-semibold text-slate-900">{title}</h3>
      {description ? <p className="mb-4 text-sm text-slate-500">{description}</p> : null}
      {actionLabel && onAction ? (
        <button type="button" onClick={onAction} className="btn-primary">
          {actionLabel}
        </button>
      ) : null}
    </div>
  )
}