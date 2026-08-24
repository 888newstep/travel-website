import { useEffect, useState, type ReactNode } from 'react'
import {
  intelligentRouteApi,
  routeCrudApi,
  type Route,
  type RouteOptimizationHistory,
  type RouteOptimizationSuggestion,
} from '../api/route.api'
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
  const [suggestions, setSuggestions] = useState<RouteOptimizationSuggestion[]>([])
  const [history, setHistory] = useState<RouteOptimizationHistory[]>([])
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
        intelligentRouteApi.getOptimizationSuggestions(routeId),
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

  async function applyOptimization(suggestion: RouteOptimizationSuggestion) {
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
              <span className="chip">{'\u8ddd\u79bb\u4f18\u5316'}</span>
              <span className="chip">{'\u987a\u5e8f\u8c03\u6574'}</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">{'\u6309\u5730\u7406\u8ddd\u79bb\u4f18\u5316\u6bcf\u65e5\u6e38\u89c8\u987a\u5e8f'}</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              {'\u4fdd\u7559\u6bcf\u65e5\u666f\u70b9\u5f52\u5c5e\uff0c\u4f7f\u7528\u666f\u70b9\u7ecf\u7eac\u5ea6\u6267\u884c\u6700\u8fd1\u90bb\u987a\u5e8f\u8c03\u6574\uff0c\u5e76\u8bb0\u5f55\u5b9e\u9645\u53d1\u751f\u7684\u53d8\u66f4\u3002'}
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
                      <h3 className="text-base font-semibold text-slate-900">{suggestion.title || suggestion.type || `\u5efa\u8bae\u7f16\u53f7 ${index + 1}`}</h3>
                      <p className="mt-2 text-sm leading-6 text-slate-500">{suggestion.description || '\u6682\u65e0\u5efa\u8bae\u8bf4\u660e'}</p>
                      <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500">
                        <span className="rounded-full bg-white px-3 py-1.5">{'\u6700\u77ed\u8ddd\u79bb'}</span>
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
              {history.map((item) => (
                <article key={`${item.routeId}-${item.appliedAt}`} className="metric-card surface-card-hover">
                  <div className="text-sm font-semibold text-slate-900">{item.optimizationType === 'distance' ? '\u6700\u77ed\u8ddd\u79bb\u4f18\u5316' : item.optimizationType}</div>
                  <p className="mt-2 text-sm leading-6 text-slate-500">{item.description || '\u6682\u65e0\u5386\u53f2\u8bf4\u660e'}</p>
                  <div className="mt-3 text-xs text-slate-400">{item.appliedAt || '\u672a\u77e5\u65f6\u95f4'}</div>
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
