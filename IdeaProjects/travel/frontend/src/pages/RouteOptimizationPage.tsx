import { useEffect, useState, type ReactNode } from 'react'
import { intelligentRouteApi, routeCrudApi, type Route } from '../api/route.api'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { DEFAULT_CITY_ID } from '../constants'

function SectionHeader({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return (
    <div className="mb-4 flex items-start justify-between gap-3">
      <div>
        <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
        <p className="mt-1 text-sm text-slate-500">{description}</p>
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  )
}

function StatTile({ label, value }: { label: string; value: number }) {
  return (
    <div className="metric-card surface-card-hover">
      <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{label}</div>
      <div className="mt-3 text-2xl font-semibold text-slate-900">{value}</div>
    </div>
  )
}

export function RouteOptimizationPage() {
  const [routes, setRoutes] = useState<Route[]>([])
  const [selectedRouteId, setSelectedRouteId] = useState(0)
  const [suggestions, setSuggestions] = useState<Record<string, any>[]>([])
  const [history, setHistory] = useState<Record<string, any>[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    void loadRoutes()
  }, [])

  useEffect(() => {
    if (selectedRouteId) {
      void loadOptimizationData(selectedRouteId)
    }
  }, [selectedRouteId])

  async function loadRoutes() {
    try {
      const data = await routeCrudApi.getRoutesByCity(DEFAULT_CITY_ID)
      const list = Array.isArray(data) ? data : []
      setRoutes(list)

      if (list.length && !selectedRouteId) {
        setSelectedRouteId(Number(list[0].id) || 0)
      }
    } catch {
      setRoutes([])
    }
  }

  async function loadOptimizationData(routeId = selectedRouteId) {
    if (!routeId) return

    setLoading(true)
    try {
      const [suggestionData, historyData] = await Promise.all([
        intelligentRouteApi.getOptimizationSuggestionsForRoute(routeId),
        intelligentRouteApi.getOptimizationHistory(routeId),
      ])

      setSuggestions(Array.isArray(suggestionData) ? suggestionData : [])
      setHistory(Array.isArray(historyData) ? historyData : [])
    } catch {
      setSuggestions([])
      setHistory([])
    } finally {
      setLoading(false)
    }
  }

  async function applyOptimization(suggestion: Record<string, any>) {
    if (!selectedRouteId || !suggestion.id) return

    try {
      await intelligentRouteApi.applyOptimizationSuggestion(selectedRouteId, suggestion.id, suggestion)
      void loadOptimizationData(selectedRouteId)
    } catch {
      // ignore for now
    }
  }

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

        <div className="relative grid gap-6 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
          <div>
            <span className="section-kicker">路线优化</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">{'\u667a\u80fd\u5efa\u8bae'}</span>
              <span className="chip">{'\u65f6\u95f4\u4f18\u5316'}</span>
              <span className="chip">{'\u884c\u7a0b\u63d0\u5347'}</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">{'\u667a\u80fd\u4f18\u5316\u4f60\u7684\u51fa\u884c\u8def\u7ebf'}</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              {'\u4ece\u73b0\u6709\u8def\u7ebf\u751f\u6210\u4f18\u5316\u5efa\u8bae\uff0c\u8f85\u52a9\u51cf\u5c11\u7ed5\u8def\u3001\u63d0\u5347\u884c\u7a0b\u5f97\u5206\uff0c\u5e76\u6c89\u6dc0\u6bcf\u6b21\u4f18\u5316\u7684\u5386\u53f2\u8bb0\u5f55\u3002'}
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <StatTile label={'\u8def\u7ebf\u6570'} value={routes.length} />
              <StatTile label={'\u5efa\u8bae\u6570'} value={suggestions.length} />
              <StatTile label={'\u5386\u53f2\u6570'} value={history.length} />
            </div>
          </div>

          <div className="scenic-shell-soft edge-glow animate-fade-in p-5 sm:p-6">
            <SectionHeader title={'\u9009\u62e9\u8def\u7ebf'} description={'\u9009\u62e9\u4e00\u6761\u8def\u7ebf\u67e5\u770b\u667a\u80fd\u4f18\u5316\u5efa\u8bae'} />
            <select
              value={selectedRouteId}
              onChange={(event) => setSelectedRouteId(Number(event.target.value) || 0)}
              className="search-input"
            >
              <option value={0}>{'\u8bf7\u9009\u62e9\u8def\u7ebf'}</option>
              {routes.map((route) => (
                <option key={route.id} value={route.id}>
                  {route.title}
                </option>
              ))}
            </select>
            <div className="mt-4 grid gap-2 text-sm text-slate-500">
              <div className="travel-step-card">{'\u6839\u636e\u5df2\u9009\u8def\u7ebf\u751f\u6210\u53ef\u6267\u884c\u7684\u4f18\u5316\u5efa\u8bae'}</div>
              <div className="travel-step-card">{'\u5e94\u7528\u5efa\u8bae\u540e\u4f1a\u81ea\u52a8\u5237\u65b0\u5efa\u8bae\u4e0e\u5386\u53f2\u8bb0\u5f55'}</div>
            </div>
          </div>
        </div>
      </section>

      {loading ? (
        <div className="mt-8">
          <LoadingSpinner />
        </div>
      ) : null}

      <div className="mt-8 grid gap-6 xl:grid-cols-2">
        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u4f18\u5316\u5efa\u8bae'} description={'\u67e5\u770b\u5f53\u524d\u8def\u7ebf\u53ef\u76f4\u63a5\u5e94\u7528\u7684\u4f18\u5316\u65b9\u6848'} action={<span className="chip">{suggestions.length} {'\u6761'}</span>} />
          {suggestions.length ? (
            <div className="space-y-4">
              {suggestions.map((suggestion, index) => (
                <article key={suggestion.id || index} className="metric-card surface-card-hover">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <h3 className="text-base font-semibold text-slate-900">{suggestion.title || suggestion.type || `\u5efa\u8bae #${index + 1}`}</h3>
                      <p className="mt-2 text-sm leading-6 text-slate-500">{suggestion.description || suggestion.detail || '\u6682\u65e0\u5efa\u8bae\u8bf4\u660e'}</p>
                      <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500">
                        {suggestion.timeSaved ? <span className="rounded-full bg-white px-3 py-1.5">{'\u8282\u7701 '}{suggestion.timeSaved}{' \u5206\u949f'}</span> : null}
                        {suggestion.costSaved ? <span className="rounded-full bg-white px-3 py-1.5">{'\u8282\u7701 \u00a5'}{suggestion.costSaved}</span> : null}
                        {suggestion.scoreImprovement ? <span className="rounded-full bg-white px-3 py-1.5">{'\u63d0\u5347 +'}{suggestion.scoreImprovement}</span> : null}
                      </div>
                    </div>
                    <button onClick={() => applyOptimization(suggestion)} className="btn-primary px-4 py-2 text-xs">{'\u5e94\u7528\u5efa\u8bae'}</button>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="py-8 text-center text-sm text-slate-400">{'\u6682\u65e0\u4f18\u5316\u5efa\u8bae'}</div>
          )}
        </section>

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u4f18\u5316\u5386\u53f2'} description={'\u67e5\u770b\u8def\u7ebf\u6700\u8fd1\u7684\u4f18\u5316\u8bb0\u5f55'} action={<span className="chip">{history.length} {'\u6761'}</span>} />
          {history.length ? (
            <div className="space-y-4">
              {history.map((item, index) => (
                <article key={item.id || index} className="metric-card surface-card-hover">
                  <div className="text-sm font-semibold text-slate-900">{item.title || item.type || `\u8bb0\u5f55 #${index + 1}`}</div>
                  <p className="mt-2 text-sm leading-6 text-slate-500">{item.description || item.detail || '\u6682\u65e0\u5386\u53f2\u8bf4\u660e'}</p>
                  <div className="mt-3 text-xs text-slate-400">{item.createTime || item.createdAt || '\u672a\u77e5\u65f6\u95f4'}</div>
                </article>
              ))}
            </div>
          ) : (
            <div className="py-8 text-center text-sm text-slate-400">{'\u6682\u65e0\u4f18\u5316\u5386\u53f2'}</div>
          )}
        </section>
      </div>
    </div>
  )
}
