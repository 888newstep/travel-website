import { useEffect, useState } from 'react'
import { routeCrudApi, type RouteTrafficResponse, type RouteTrafficStatus } from '../../api/route.api'
import { isRequestTimeoutError, withRequestTimeout } from '../../lib/request'

const STATUS_LABELS: Record<RouteTrafficStatus, string> = {
  unknown: '未知',
  light: '畅通',
  moderate: '缓行',
  heavy: '拥堵',
  severe: '严重拥堵',
}

function formatDistance(meters: number) {
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)} 公里` : `${meters} 米`
}

function formatDuration(seconds: number) {
  const minutes = Math.max(1, Math.round(seconds / 60))
  return minutes >= 60 ? `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分钟` : `${minutes} 分钟`
}

export function RouteTrafficSummary({ routeId }: { routeId: number }) {
  const [traffic, setTraffic] = useState<RouteTrafficResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    setMessage(null)
    withRequestTimeout(routeCrudApi.getRouteTraffic(routeId))
      .then((result) => {
        if (!active) return
        setTraffic(result)
        if (!result.dataAvailable) setMessage(result.message || '实时路况暂不可用。')
      })
      .catch((error) => {
        if (!active) return
        setTraffic(null)
        setMessage(isRequestTimeoutError(error) ? '实时路况加载超时。' : '实时路况加载失败。')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [routeId])

  return (
    <section className="border-t border-slate-200 pt-5">
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-base font-semibold text-slate-900">实时驾车路况</h3>
        <span className="chip">高德</span>
      </div>
      {loading ? <div className="mt-3 text-sm text-slate-500">正在计算路线...</div> : null}
      {!loading && message ? <div className="mt-3 text-sm text-amber-700">{message}</div> : null}
      {!loading && traffic?.dataAvailable ? (
        <>
          <div className="mt-4 grid grid-cols-2 gap-3">
            <div className="metric-card"><div className="text-xs text-slate-400">驾车距离</div><div className="mt-2 font-semibold text-slate-900">{formatDistance(traffic.totalDistanceMeters)}</div></div>
            <div className="metric-card"><div className="text-xs text-slate-400">预计耗时</div><div className="mt-2 font-semibold text-slate-900">{formatDuration(traffic.totalDurationSeconds)}</div></div>
          </div>
          <div className="mt-4 space-y-2">
            {traffic.segments.map((segment, index) => (
              <div key={segment.segmentId || index} className="flex items-center justify-between gap-3 border-t border-slate-100 py-3 text-sm">
                <span className="text-slate-600">第 {index + 1} 段 · {formatDistance(segment.distanceMeters)}</span>
                <span className="font-medium text-slate-900">{STATUS_LABELS[segment.status] || STATUS_LABELS.unknown}</span>
              </div>
            ))}
          </div>
        </>
      ) : null}
    </section>
  )
}
