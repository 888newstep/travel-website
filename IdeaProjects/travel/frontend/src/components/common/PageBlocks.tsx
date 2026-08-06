import type { ChangeEvent, PropsWithChildren, ReactNode } from 'react'

interface SectionCardProps extends PropsWithChildren {
  title: string
  description?: string
  action?: ReactNode
  className?: string
}

interface StatCardProps {
  label: string
  value: ReactNode
  hint?: ReactNode
  className?: string
}

interface StatsGridProps extends PropsWithChildren {
  className?: string
}

interface SearchPanelProps {
  title: string
  headline?: string
  value: string
  placeholder: string
  onChange: (value: string) => void
  meta?: ReactNode
  footer?: ReactNode
  className?: string
  inputClassName?: string
}

export function SectionCard({
  title,
  description,
  action,
  className = '',
  children,
}: SectionCardProps) {
  return (
    <section className={`surface-card edge-glow animate-fade-in rounded-[1.75rem] p-6 ${className}`.trim()}>
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
          {description ? <p className="mt-1 text-sm text-slate-500">{description}</p> : null}
        </div>
        {action ? <div className="shrink-0">{action}</div> : null}
      </div>
      {children}
    </section>
  )
}

export function StatCard({ label, value, hint, className = '' }: StatCardProps) {
  return (
    <div className={`surface-card edge-glow rounded-2xl px-4 py-4 ${className}`.trim()}>
      <div className="text-xs text-slate-500">{label}</div>
      <div className="mt-2 text-2xl font-semibold text-slate-900">{value}</div>
      {hint ? <div className="mt-2 text-xs text-slate-400">{hint}</div> : null}
    </div>
  )
}

export function StatsGrid({ className = '', children }: StatsGridProps) {
  return <section className={`grid gap-5 lg:grid-cols-3 ${className}`.trim()}>{children}</section>
}

export function SearchPanel({
  title,
  headline,
  value,
  placeholder,
  onChange,
  meta,
  footer,
  className = '',
  inputClassName = '',
}: SearchPanelProps) {
  return (
    <div className={`surface-card edge-glow animate-fade-in rounded-[1.5rem] p-5 ${className}`.trim()}>
      <div className="text-sm font-medium text-slate-500">{title}</div>
      {headline ? <div className="mt-1 text-xl font-semibold text-slate-900">{headline}</div> : null}
      <input
        value={value}
        onChange={(event: ChangeEvent<HTMLInputElement>) => onChange(event.target.value)}
        placeholder={placeholder}
        className={`search-input mt-3 ${inputClassName}`.trim()}
      />
      {meta ? <div className="mt-4">{meta}</div> : null}
      {footer ? <div className="mt-4">{footer}</div> : null}
    </div>
  )
}
