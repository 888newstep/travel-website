import type { PropsWithChildren, ReactNode } from 'react'

interface DetailTextCardProps extends PropsWithChildren {
  title?: string
  className?: string
}

interface DetailMetricItem {
  label: string
  value: ReactNode
}

interface DetailMetricsGridProps {
  items: DetailMetricItem[]
  columnsClassName?: string
  className?: string
}

export function DetailTextCard({ title, className = '', children }: DetailTextCardProps) {
  return (
    <div className={`rounded-2xl bg-slate-50 px-4 py-4 text-sm text-slate-600 ${className}`.trim()}>
      {title ? <div className="text-xs text-slate-500">{title}</div> : null}
      <div className={title ? 'mt-2 leading-6' : 'leading-7'}>{children}</div>
    </div>
  )
}

export function DetailMetricsGrid({
  items,
  columnsClassName = 'sm:grid-cols-2 xl:grid-cols-4',
  className = '',
}: DetailMetricsGridProps) {
  return (
    <div className={`grid gap-3 ${columnsClassName} ${className}`.trim()}>
      {items.map((item) => (
        <div key={item.label} className="rounded-2xl bg-slate-50 px-4 py-4 text-sm text-slate-600">
          <div className="text-xs text-slate-500">{item.label}</div>
          <div className="mt-2 font-medium text-slate-900">{item.value}</div>
        </div>
      ))}
    </div>
  )
}
