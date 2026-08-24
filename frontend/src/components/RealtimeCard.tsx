import { CROWD_LEVEL_HIGH, CROWD_LEVEL_MEDIUM } from '../constants'

interface RealtimeCardProps {
  data: Record<string, any>
}

function getStatusLabel(data: Record<string, any>) {
  if (!data.openStatus) return '\u5df2\u5173\u95ed'
  const level = data.crowdLevel ?? 0
  if (level >= CROWD_LEVEL_HIGH) return '\u62e5\u6324'
  if (level >= CROWD_LEVEL_MEDIUM) return '\u9002\u4e2d'
  return '\u8212\u9002'
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
          <p className="text-xs font-medium uppercase tracking-[0.24em] text-slate-400">{'\u5b9e\u65f6\u72b6\u6001'}</p>
          <h3 className="mt-2 text-base font-semibold text-slate-900">{'\u666f\u70b9\u7f16\u53f7 '}{data.attractionId}</h3>
        </div>
        <span className={`rounded-full px-3 py-1 text-xs font-medium ${getStatusClass(data)}`}>{getStatusLabel(data)}</span>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <div className="rounded-2xl bg-slate-50/80 px-4 py-3">
          <p className="text-xs text-slate-400">{'\u62e5\u6324\u4eba\u6570'}</p>
          <p className="mt-1 text-sm font-semibold text-slate-700">{data.crowdCount ?? '-'} {'\u00b7 \u7b49\u7ea7 '} {data.crowdLevel ?? '-'}</p>
        </div>
        <div className="rounded-2xl bg-slate-50/80 px-4 py-3">
          <p className="text-xs text-slate-400">{'\u7b49\u5f85\u65f6\u95f4'}</p>
          <p className="mt-1 text-sm font-semibold text-slate-700">{data.waitTime != null ? `${data.waitTime} \u5206\u949f` : '-'}</p>
        </div>
        <div className="rounded-2xl bg-slate-50/80 px-4 py-3">
          <p className="text-xs text-slate-400">{'\u6e29\u5ea6'}</p>
          <p className="mt-1 text-sm font-semibold text-slate-700">{data.temperature != null ? `${data.temperature}\u00b0C` : '-'}</p>
        </div>
        <div className="rounded-2xl bg-slate-50/80 px-4 py-3">
          <p className="text-xs text-slate-400">{'\u5929\u6c14'}</p>
          <p className="mt-1 text-sm font-semibold text-slate-700">{data.weather || '-'}</p>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2 text-xs text-slate-500">
        <span className="rounded-full bg-sky-100 px-3 py-1.5">{'\u72b6\u6001\uff1a'}{data.status || '-'}</span>
        <span className={`rounded-full px-3 py-1.5 ${data.openStatus ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-600'}`}>
          {data.openStatus ? '\u8425\u4e1a\u4e2d' : '\u5df2\u5173\u95ed'}
        </span>
      </div>

      {data.lastUpdateTime ? <p className="mt-4 text-xs text-slate-400">{'\u6700\u8fd1\u66f4\u65b0\uff1a'}{new Date(data.lastUpdateTime).toLocaleString()}</p> : null}
    </article>
  )
}
