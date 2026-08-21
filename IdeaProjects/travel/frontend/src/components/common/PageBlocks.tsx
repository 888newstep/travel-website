import type { PropsWithChildren, ReactNode } from 'react'

interface StatCardProps {
  label: string
  value: ReactNode
  hint?: ReactNode
  className?: string
}

interface StatsGridProps extends PropsWithChildren {
  className?: string
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
