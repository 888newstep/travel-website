import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { attractionApi, type Attraction } from '../api/attraction.api'
import { noteApi, type TravelNote } from '../api/note.api'
import { routeCrudApi, type Route } from '../api/route.api'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { DEFAULT_CITY_ID, TEXT } from '../constants'

function firstImage(item: Attraction) {
  return Array.isArray(item.images) ? item.images[0] || '' : ''
}

export function HomePage() {
  const [loading, setLoading] = useState(true)
  const [attractions, setAttractions] = useState<Attraction[]>([])
  const [routes, setRoutes] = useState<Route[]>([])
  const [notes, setNotes] = useState<TravelNote[]>([])
  const [recommended, setRecommended] = useState<Attraction[]>([])

  useEffect(() => {
    let active = true
    Promise.allSettled([
      attractionApi.getAttractions(),
      routeCrudApi.getRoutesByCity(DEFAULT_CITY_ID),
      noteApi.getLatestNotes(3),
      attractionApi.getRecommendations(DEFAULT_CITY_ID, 3),
    ]).then(([a, r, n, rec]) => {
      if (!active) return
      setAttractions(a.status === 'fulfilled' && Array.isArray(a.value) ? a.value : [])
      setRoutes(r.status === 'fulfilled' && Array.isArray(r.value) ? r.value : [])
      setNotes(n.status === 'fulfilled' && Array.isArray(n.value) ? n.value : [])
      setRecommended(rec.status === 'fulfilled' && Array.isArray(rec.value) ? rec.value : [])
      setLoading(false)
    })
    return () => {
      active = false
    }
  }, [])

  const stats = useMemo(
    () => [
      ['景点', attractions.length, '灵感来源'],
      ['路线', routes.length, '行程参考'],
      ['游记', notes.length, '真实体验'],
    ],
    [attractions.length, routes.length, notes.length],
  )

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />
        <div className="relative grid gap-6 xl:grid-cols-[1.08fr_0.92fr] xl:items-start">
          <div>
            <span className="section-kicker">智慧旅行</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">旅游灵感</span>
              <span className="chip">路线整理</span>
              <span className="chip">Edge 友好</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-5xl">
              从灵感出发，轻松拼出你的旅行路线
            </h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              把景点、路线、游记和 AI 助手串在一起，帮助你更快完成一次可落地、可调整、可回看的旅行规划。
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link to="/attractions" className="btn-primary">先看景点</Link>
              <Link to="/routes" className="btn-secondary">浏览路线</Link>
            </div>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              {stats.map(([label, value, caption]) => (
                <div key={String(label)} className="metric-card">
                  <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{label}</div>
                  <div className="mt-3 text-3xl font-semibold text-slate-900">{value}</div>
                  <p className="mt-2 text-sm text-slate-500">{caption}</p>
                </div>
              ))}
            </div>
          </div>
          <div className="scenic-shell-soft p-6">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <div className="text-sm font-medium text-slate-500">快速入口</div>
                <div className="mt-1 text-xl font-semibold text-slate-900">先看景点，再让 AI 帮你串路线</div>
              </div>
              <span className="chip">高频功能</span>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <Link to="/attractions" className="travel-step-card">Attractions</Link>
              <Link to="/routes" className="travel-step-card">路线规划</Link>
              <Link to="/notes" className="travel-step-card">真实游记</Link>
              <Link to="/ai-chat" className="travel-step-card">AI 助手</Link>
            </div>
          </div>
        </div>
      </section>

      <section className="mt-10 grid gap-8 xl:grid-cols-2">
        <div>
          <div className="mb-6 flex items-end justify-between gap-3">
            <div>
              <span className="section-kicker">精选景点</span>
              <h2 className="mt-3 section-heading">精选景点</h2>
              <p className="section-subtitle mt-2">优先看看适合本次出游节奏的热门推荐。</p>
            </div>
            <Link to="/attractions" className="feature-link">查看更多 →</Link>
          </div>
          {loading ? <LoadingSpinner /> : null}
          {!loading && recommended.length ? (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {recommended.map((item) => (
                <article key={item.id} className="scenic-shell-soft surface-card-hover overflow-hidden">
                  {firstImage(item) ? <img src={firstImage(item)} alt={item.name} className="h-44 w-full object-cover" /> : <div className="h-44 bg-gradient-to-br from-sky-100 via-white to-emerald-100" />}
                  <div className="p-5">
                    <div className="flex items-center justify-between gap-3">
                      <h3 className="text-lg font-semibold text-slate-900">{item.name}</h3>
                      <span className="chip">推荐</span>
                    </div>
                    <p className="mt-3 line-clamp-2 text-sm leading-6 text-slate-500">{item.description || TEXT.NO_DESCRIPTION}</p>
                    <div className="mt-4 flex items-center justify-between text-sm">
                      <span className="text-slate-400">{item.rating ? `⭐ ${item.rating}` : TEXT.NO_RATING}</span>
                      <Link to="/attractions" className="feature-link">查看详情 →</Link>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : null}
        </div>

        <div className="grid gap-8">
          <div>
            <div className="mb-6 flex items-end justify-between gap-3">
              <div>
                <span className="section-kicker">推荐路线</span>
                <h2 className="mt-3 section-heading">推荐路线</h2>
              </div>
              <Link to="/routes" className="feature-link">查看更多 →</Link>
            </div>
            {!loading && routes.length ? (
              <div className="grid gap-4">
                {routes.slice(0, 3).map((item) => (
                  <div key={item.id} className="scenic-shell-soft surface-card-hover p-5">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <div className="text-lg font-semibold text-slate-900">{item.title}</div>
                        <p className="mt-2 text-sm leading-6 text-slate-500">{item.description || '这条路线暂时没有简介，可进入详情页继续查看完整信息。'}</p>
                      </div>
                      <span className="chip">{item.durationDays ? `${item.durationDays} 天` : '未设置'}</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : null}
          </div>
          <div>
            <div className="mb-6 flex items-end justify-between gap-3">
              <div>
                <span className="section-kicker">最新游记</span>
                <h2 className="mt-3 section-heading">最新游记</h2>
              </div>
              <Link to="/notes" className="feature-link">查看更多 →</Link>
            </div>
            {!loading && notes.length ? (
              <div className="grid gap-4">
                {notes.slice(0, 3).map((item) => (
                  <article key={item.id} className="scenic-shell-soft surface-card-hover p-5">
                    <div className="mb-3 flex items-center justify-between text-xs text-slate-400">
                      <span>{item.author || TEXT.ANONYMOUS}</span>
                      <span>点赞 {item.likes || 0} · 评论 {item.comments || 0}</span>
                    </div>
                    <h3 className="text-lg font-semibold text-slate-900">{item.title}</h3>
                    <p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-500">{item.excerpt || item.content || TEXT.NO_CONTENT}</p>
                  </article>
                ))}
              </div>
            ) : null}
          </div>
        </div>
      </section>
    </div>
  )
}
