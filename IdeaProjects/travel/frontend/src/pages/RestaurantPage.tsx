import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { restaurantApi, type Restaurant } from '../api/restaurant.api'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { DEFAULT_CITY_ID } from '../constants'

function formatCost(value?: number) {
  if (!value) return '\u672a\u63d0\u4f9b'
  return `\u00a5${value}/\u4eba`
}

function getTopCuisineTypes(restaurants: Restaurant[]) {
  const counts = new Map<string, number>()

  restaurants.forEach((item) => {
    const type = item.cuisineType?.trim()
    if (!type) return
    counts.set(type, (counts.get(type) || 0) + 1)
  })

  return Array.from(counts.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 4)
    .map(([name]) => name)
}

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

function StatTile({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="metric-card surface-card-hover">
      <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{label}</div>
      <div className="mt-3 text-2xl font-semibold text-slate-900">{value}</div>
    </div>
  )
}

export function RestaurantPage() {
  const [loading, setLoading] = useState(true)
  const [restaurants, setRestaurants] = useState<Restaurant[]>([])
  const [keyword, setKeyword] = useState('')

  async function fetchRestaurants(searchText?: string) {
    setLoading(true)
    try {
      const normalized = (searchText ?? keyword).trim()
      const data = normalized ? await restaurantApi.search(DEFAULT_CITY_ID, normalized) : await restaurantApi.getByCity(DEFAULT_CITY_ID)
      setRestaurants(Array.isArray(data) ? data : [])
    } catch {
      setRestaurants([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRestaurants('')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const topCuisineTypes = useMemo(() => getTopCuisineTypes(restaurants), [restaurants])
  const highRatedCount = useMemo(() => restaurants.filter((item) => Number(item.rating) >= 4.5).length, [restaurants])
  const highRatedRatio = restaurants.length ? `${Math.round((highRatedCount / restaurants.length) * 100)}%` : '0%'

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

        <div className="relative grid gap-6 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
          <div>
            <span className="section-kicker">当地美食</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">{'\u672c\u5730\u7cbe\u9009'}</span>
              <span className="chip">{'\u7f8e\u98df\u5730\u56fe'}</span>
              <span className="chip">{'\u7528\u9910\u53c2\u8003'}</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">{'\u53d1\u73b0\u65c5\u9014\u4e2d\u7684\u5f53\u5730\u597d\u5473\u9053'}</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              {'\u6c47\u603b\u57ce\u5e02\u9910\u5385\u3001\u70ed\u95e8\u83dc\u7cfb\u4e0e\u8425\u4e1a\u4fe1\u606f\uff0c\u5e2e\u52a9\u4f60\u5728\u51fa\u884c\u9014\u4e2d\u5feb\u901f\u627e\u5230\u66f4\u9002\u5408\u5f53\u524d\u8282\u594f\u7684\u7528\u9910\u9009\u62e9\u3002'}
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <StatTile label={'\u9910\u5385\u6570'} value={restaurants.length} />
              <StatTile label={'\u641c\u7d22\u72b6\u6001'} value={keyword.trim() ? '\u641c\u7d22\u4e2d' : '\u5168\u90e8'} />
              <StatTile label={'\u9ad8\u8bc4\u5206'} value={highRatedCount} />
              <StatTile label={'\u57ce\u5e02 ID'} value={`#${DEFAULT_CITY_ID}`} />
            </div>
            {topCuisineTypes.length ? (
              <div className="mt-5 flex flex-wrap gap-2">
                {topCuisineTypes.map((item) => (
                  <span key={item} className="chip">{'\u70ed\u95e8 \u00b7 '}{item}</span>
                ))}
              </div>
            ) : null}
          </div>

          <div className="scenic-shell-soft edge-glow animate-fade-in p-5 sm:p-6">
            <SectionHeader
              title={'\u5feb\u901f\u7b5b\u9009'}
              description={'\u8f93\u5165\u5173\u952e\u8bcd\u67e5\u627e\u5e97\u540d\u3001\u83dc\u7cfb\u6216\u5730\u5740'}
              action={<span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">{'\u7f8e\u98df'}</span>}
            />
            <div className="flex flex-col gap-3 sm:flex-row">
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                type="text"
                placeholder={'\u641c\u7d22\u9910\u5385\u540d\u79f0\u3001\u83dc\u7cfb\u6216\u5730\u5740'}
                className="search-input"
              />
              <button type="button" onClick={() => fetchRestaurants()} className="btn-primary">
                {'\u67e5\u8be2'}
              </button>
              <button type="button" onClick={() => fetchRestaurants('')} className="btn-secondary">
                {'\u91cd\u7f6e'}
              </button>
            </div>
            <div className="mt-4 text-sm text-slate-500">
              {'\u5f53\u524d\u663e\u793a\uff1a'}{keyword.trim() ? '\u5173\u952e\u8bcd\u5df2\u5e94\u7528' : '\u6d4f\u89c8\u5168\u90e8'} {'\u00b7 \u57ce\u5e02 #'}{DEFAULT_CITY_ID}
            </div>
          </div>
        </div>
      </section>

      <section className="mt-8">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u9009\u62e9\u5efa\u8bae'} description={'\u7ed3\u5408\u65c5\u6e38\u573a\u666f\uff0c\u4f18\u5148\u5173\u6ce8\u8bc4\u5206\u3001\u8425\u4e1a\u65f6\u95f4\u4e0e\u53e3\u5473\u5339\u914d\u5ea6'} />
          <div className="mt-5 grid gap-4 md:grid-cols-3">
            <div className="travel-step-card">
              <div className="text-base font-semibold text-slate-900">{'\u5148\u770b\u53e3\u5473'}</div>
              <p className="mt-2 leading-6 text-slate-500">{'\u901a\u8fc7\u70ed\u95e8\u83dc\u7cfb\u5feb\u901f\u7b5b\u9009\uff0c\u66f4\u5bb9\u6613\u627e\u5230\u7b26\u5408\u5f53\u524d\u65c5\u7a0b\u504f\u597d\u7684\u9910\u5385\u3002'}</p>
            </div>
            <div className="travel-step-card">
              <div className="text-base font-semibold text-slate-900">{'\u518d\u770b\u8bc4\u5206'}</div>
              <p className="mt-2 leading-6 text-slate-500">{'\u4f18\u5148\u67e5\u770b\u9ad8\u8bc4\u5206\u5546\u5bb6\uff0c\u51cf\u5c11\u4e34\u65f6\u8e29\u5751\uff0c\u63d0\u9ad8\u7528\u9910\u4f53\u9a8c\u7a33\u5b9a\u6027\u3002'}</p>
            </div>
            <div className="travel-step-card">
              <div className="text-base font-semibold text-slate-900">{'\u786e\u8ba4\u8425\u4e1a'}</div>
              <p className="mt-2 leading-6 text-slate-500">{'\u7ed3\u5408\u8425\u4e1a\u65f6\u95f4\u4e0e\u8054\u7cfb\u65b9\u5f0f\uff0c\u907f\u514d\u5230\u5e97\u540e\u4e34\u65f6\u6251\u7a7a\u6216\u6392\u961f\u8fc7\u4e45\u3002'}</p>
            </div>
          </div>
        </div>
      </section>

      <section className="mt-8">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u6570\u636e\u6982\u89c8'} description={'\u4ece\u5f53\u524d\u7b5b\u9009\u7ed3\u679c\u4e2d\u5feb\u901f\u67e5\u770b\u53ef\u7528\u9910\u5385\u7684\u6574\u4f53\u5206\u5e03'} />
          <div className="mt-5 space-y-3 text-sm text-slate-600">
            <div className="metric-card surface-card-hover flex items-center justify-between">
              <span>{'\u5f53\u524d\u57ce\u5e02'}</span>
              <span className="font-semibold text-slate-900">#{DEFAULT_CITY_ID}</span>
            </div>
            <div className="metric-card surface-card-hover flex items-center justify-between">
              <span>{'\u68c0\u7d22\u6a21\u5f0f'}</span>
              <span className="font-semibold text-slate-900">{keyword.trim() ? '\u5173\u952e\u8bcd\u641c\u7d22' : '\u57ce\u5e02\u5168\u91cf'}</span>
            </div>
            <div className="metric-card surface-card-hover flex items-center justify-between">
              <span>{'\u9ad8\u5206\u5360\u6bd4'}</span>
              <span className="font-semibold text-slate-900">{highRatedRatio}</span>
            </div>
          </div>
        </div>
      </section>

      <div className="mb-6 mt-10 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="section-heading text-[1.75rem]">{'\u9910\u5385\u5217\u8868'}</h2>
          <p className="section-subtitle mt-2">{'\u652f\u6301\u67e5\u770b\u8bc4\u5206\u3001\u83dc\u7cfb\u3001\u4eba\u5747\u6d88\u8d39\u4e0e\u8425\u4e1a\u65f6\u95f4\u4fe1\u606f\u3002'}</p>
        </div>
      </div>

      {loading ? <LoadingSpinner /> : null}
      {!loading && !restaurants.length ? (
        <div className="scenic-shell-soft edge-glow animate-fade-in p-10 text-center text-sm text-slate-500">
          {'\u6682\u65e0\u7b26\u5408\u6761\u4ef6\u7684\u9910\u5385'}
        </div>
      ) : null}
      {!loading && restaurants.length ? (
        <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {restaurants.map((item, index) => (
            <article
              key={item.id}
              className="scenic-shell-soft surface-card-hover animate-scale-in overflow-hidden"
              style={{ animationDelay: `${index * 60}ms` }}
            >
              {item.imageUrl ? (
                <div className="relative overflow-hidden">
                  <img src={item.imageUrl} alt={item.name} className="h-44 w-full object-cover transition duration-500 hover:scale-[1.03]" />
                  <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-slate-900/45 to-transparent" />
                </div>
              ) : (
                <div className="relative h-44 bg-gradient-to-br from-sky-100 via-white to-emerald-100">
                  <div className="scenic-orb scenic-orb-sky right-4 top-4 h-24 w-24 opacity-60" />
                </div>
              )}
              <div className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="text-lg font-semibold text-slate-900">{item.name}</h3>
                    <p className="mt-1 text-xs text-slate-400">{item.address}</p>
                  </div>
                  <span className="rounded-full bg-sky-100 px-2.5 py-1 text-xs font-medium text-slate-500">{'\u2605 '}{item.rating || '--'}</span>
                </div>
                <p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-500">{item.description || item.feature || '\u6682\u65e0\u9910\u5385\u4ecb\u7ecd'}</p>
                <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-400">
                  <span className="chip">{item.cuisineType || '\u672a\u5206\u7c7b'}</span>
                  <span className="chip">{item.priceLevel || formatCost(item.averageCost)}</span>
                </div>
                <div className="mt-4 space-y-1 text-xs text-slate-400">
                  <div>{'\u4eba\u5747\uff1a'}{formatCost(item.averageCost)}</div>
                  <div>{'\u8425\u4e1a\uff1a'}{item.openingHours || '\u672a\u63d0\u4f9b'}</div>
                  <div>{'\u7535\u8bdd\uff1a'}{item.phone || '\u672a\u63d0\u4f9b'}</div>
                </div>
              </div>
            </article>
          ))}
        </section>
      ) : null}
    </div>
  )
}
