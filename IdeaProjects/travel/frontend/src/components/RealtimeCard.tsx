import { CROWD_LEVEL_HIGH, CROWD_LEVEL_MEDIUM } from '../constants'

interface RealtimeCardProps {
  data: Record<string, any>
}

function getStatusLabel(data: Record<string, any>) {
  if (!data.openStatus) return '???'
  const level = data.crowdLevel ?? 0
  if (level >= CROWD_LEVEL_HIGH) return '??'
  if (level >= CROWD_LEVEL_MEDIUM) return '??'
  return '??'
}

function getStatusClass(data: Record<string, any>) {
  if (!data.openStatus) return 'bg-rose-50 text-rose-600'
  const level = data.crowdLevel ?? 0
  if (level >= CROWD_LEVEL_HIGH) return 'bg-rose-50 text-rose-600'
  if (level >= CROWD_LEVEL_MEDIUM) return 'bg-sky-100 text-sky-700'
  return 'bg-emerald-50 text-emerald-700'
}

export function RealtimeCard({ data }: RealtimeCardProps) {
  return (
    <article className="surface-card surface-card-hover overflow-hidden rounded-[1.5rem] p-4 sm:p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.24em] text-slate-400">MessageList</p>
          <h3 className="mt-2 text-base font-semibold text-slate-900">?? #{data.attractionId}</h3>
        </div>
        <span className={`rounded-full px-3 py-1 text-xs font-medium ${getStatusClass(data)}`}>{getStatusLabel(data)}</span>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <div className="rounded-2xl bg-slate-50/80 px-4 py-3">
          <p className="text-xs text-slate-400">MessageList</p>
          <p className="mt-1 text-sm font-semibold text-slate-700">{data.crowdCount ?? '-'} ? ?? {data.crowdLevel ?? '-'}</p>
        </div>
        <div className="rounded-2xl bg-slate-50/80 px-4 py-3">
          <p className="text-xs text-slate-400">MessageList</p>
          <p className="mt-1 text-sm font-semibold text-slate-700">{data.waitTime != null ? `${data.waitTime} ??` : '-'}</p>
        </div>
        <div className="rounded-2xl bg-slate-50/80 px-4 py-3">
          <p className="text-xs text-slate-400">??</p>
          <p className="mt-1 text-sm font-semibold text-slate-700">{data.temperature != null ? `${data.temperature}?C` : '-'}</p>
        </div>
        <div className="rounded-2xl bg-slate-50/80 px-4 py-3">
          <p className="text-xs text-slate-400">??</p>
          <p className="mt-1 text-sm font-semibold text-slate-700">{data.weather || '-'}</p>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2 text-xs text-slate-500">
        <span className="rounded-full bg-sky-100 px-3 py-1.5">???{data.status || '-'}</span>
        <span className={`rounded-full px-3 py-1.5 ${data.openStatus ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-600'}`}>
          {data.openStatus ? '???' : '???'}
        </span>
      </div>

      {data.lastUpdateTime ? <p className="mt-4 text-xs text-slate-400">MessageList?{new Date(data.lastUpdateTime).toLocaleString()}</p> : null}
    </article>
  )
}
