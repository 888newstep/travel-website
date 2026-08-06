import type { ReactNode } from 'react'

interface RelatedSectionProps {
  title: string
  emptyText: string
  hasItems: boolean
  children: ReactNode
}

interface RelatedCardProps {
  title: string
  description: string
  side?: ReactNode
  onClick?: () => void
}

export function RelatedSection({ title, emptyText, hasItems, children }: RelatedSectionProps) {
  return (
    <div>
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      {hasItems ? (
        <div className="mt-3 grid gap-3 sm:grid-cols-2">{children}</div>
      ) : (
        <div className="mt-3 rounded-2xl border border-dashed border-slate-200 px-4 py-4 text-sm text-slate-400">{emptyText}</div>
      )}
    </div>
  )
}

export function RelatedCard({ title, description, side, onClick }: RelatedCardProps) {
  const className = onClick
    ? 'rounded-2xl border border-slate-200 bg-white px-4 py-4 text-left transition hover:border-sky-200 hover:bg-sky-50/50'
    : 'rounded-2xl bg-slate-50 px-4 py-4 text-sm text-slate-600'

  const content = (
    <>
      <div className="flex items-center justify-between gap-3">
        <div className="font-medium text-slate-900">{title}</div>
        {side ? <span className="text-xs text-slate-400">{side}</span> : null}
      </div>
      <div className="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">{description}</div>
    </>
  )

  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={className}>
        {content}
      </button>
    )
  }

  return <div className={className}>{content}</div>
}
