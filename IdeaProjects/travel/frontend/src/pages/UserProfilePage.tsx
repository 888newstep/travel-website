import { useEffect, useMemo, useState } from 'react'

import { useNavigate } from 'react-router-dom'

import { collectionApi, type RouteCollectionVO } from '../api/collection.api'

import { noteApi, type TravelNote } from '../api/note.api'

import { routeCrudApi, type Route } from '../api/route.api'

import { userApi, type User } from '../api/user.api'

import { userStatsApi, type UserStats } from '../api/user-stats.api'

import { LoadingSpinner } from '../components/common/LoadingSpinner'

import { EXCERPT_MAX_LENGTH, TEXT } from '../constants'

import { clearStoredAuth } from '../lib/auth'



function truncateText(value?: string) {

  if (!value) {

    return TEXT.NO_DESCRIPTION

  }



  return value.length > EXCERPT_MAX_LENGTH ? `${value.slice(0, EXCERPT_MAX_LENGTH)}...` : value

}



export function UserProfilePage() {

  const navigate = useNavigate()

  const [loading, setLoading] = useState(true)

  const [user, setUser] = useState<User | null>(null)

  const [stats, setStats] = useState<UserStats>({})

  const [collections, setCollections] = useState<RouteCollectionVO[]>([])

  const [myRoutes, setMyRoutes] = useState<Route[]>([])

  const [myNotes, setMyNotes] = useState<TravelNote[]>([])



  useEffect(() => {

    let active = true



    async function loadProfile() {

      try {

        const currentUser = await userApi.getCurrentUser()

        if (!active) {

          return

        }



        setUser(currentUser)

        if (!currentUser?.id) {

          return

        }



        const [statsResult, collectionResult, routeResult, noteResult] = await Promise.allSettled([

          userStatsApi.getCurrentUserStats(),

          collectionApi.getUserCollections(currentUser.id),

          routeCrudApi.getMyRoutes(currentUser.id),

          noteApi.getUserTravelNotes(currentUser.id),

        ])



        if (!active) {

          return

        }



        if (statsResult.status === 'fulfilled') setStats(statsResult.value || {})

        if (collectionResult.status === 'fulfilled') setCollections(Array.isArray(collectionResult.value) ? collectionResult.value : [])

        if (routeResult.status === 'fulfilled') setMyRoutes(Array.isArray(routeResult.value) ? routeResult.value : [])

        if (noteResult.status === 'fulfilled') setMyNotes(Array.isArray(noteResult.value) ? noteResult.value : [])

      } catch {

        if (active) {

          setUser(null)

        }

      } finally {

        if (active) {

          setLoading(false)

        }

      }

    }



    loadProfile()



    return () => {

      active = false

    }

  }, [])



  const overviewCards = useMemo(

    () => [

      { label: '我的路线', value: stats.totalRoutes || 0, caption: '行程沉淀' },

      { label: '旅行笔记', value: stats.totalNotes || 0, caption: '记录灵感' },

      { label: '收藏数量', value: stats.totalCollections || 0, caption: '常看常新' },

      { label: '收到点赞', value: stats.totalLikes || 0, caption: '互动反馈' },

    ],

    [stats],

  )



  async function handleRemoveCollection(item: RouteCollectionVO) {

    if (!user?.id || !item.routeId) {

      return

    }



    try {

      await collectionApi.removeCollection(item.routeId)

      setCollections((current) => current.filter((entry) => entry.id !== item.id))

    } catch {

      // ignore for now

    }

  }



  function handleLogout() {

    clearStoredAuth()

    navigate('/login', { replace: true })

  }



  if (loading) {

    return (

      <div className="app-container pb-16 pt-4 md:pt-6">

        <section className="scenic-shell">

          <LoadingSpinner />

        </section>

      </div>

    )

  }



  return (

    <div className="app-container pb-16 pt-4 md:pt-6">

      <section className="scenic-shell edge-glow animate-slide-up px-6 py-7 sm:px-8 sm:py-8">

        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />

        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />



        <div className="relative grid gap-6 xl:grid-cols-[1.05fr_0.95fr] xl:items-center">

          <div>

            <span className="section-kicker">{'\u4e2a\u4eba\u8d44\u6599'}</span>

            <div className="mt-3 flex flex-wrap gap-2">

              <span className="chip">个人中心</span>

              <span className="chip">旅行资产</span>

              <span className="chip">行程记录</span>

            </div>

            <div className="mt-5 flex items-center gap-4">

              <div className="flex h-18 w-18 items-center justify-center rounded-[1.5rem] bg-gradient-to-br from-sky-500 to-emerald-500 text-2xl font-semibold text-white shadow-lg shadow-sky-500/20">

                {(user?.username || 'U').charAt(0).toUpperCase()}

              </div>

              <div>

                <h1 className="text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">

                  {user?.username || '未登录用户'}

                </h1>

                <p className="mt-2 text-sm text-slate-500">{user?.phone || '未绑定手机号'}</p>

                <p className="mt-1 text-sm text-slate-400">

                  {user?.role === 'admin' ? '管理员账号' : '旅行用户'}

                </p>

              </div>

            </div>

            <p className="mt-5 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">

              在这里集中查看你的路线、收藏与游记沉淀，方便继续完善下一次出发前的准备与复盘。

            </p>

            <div className="mt-5 flex flex-wrap gap-3">

              <button type="button" onClick={() => navigate('/routes')} className="btn-primary">

                查看路线

              </button>

              <button type="button" onClick={handleLogout} className="btn-secondary">

                退出登录

              </button>

            </div>

          </div>



          <div className="grid gap-4 sm:grid-cols-2">

            {overviewCards.map((item) => (

              <div key={item.label} className="metric-card">

                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{item.label}</div>

                <div className="mt-3 text-3xl font-semibold text-slate-900">{item.value}</div>

                <p className="mt-2 text-sm text-slate-500">{item.caption}</p>

              </div>

            ))}

          </div>

        </div>

      </section>



      <section className="mb-6 mt-8 grid gap-6 xl:grid-cols-[1fr_1fr]">

        <div className="scenic-shell-soft p-6">

          <div className="mb-4 flex items-center justify-between gap-3">

            <div>

              <span className="section-kicker">{'\u6536\u85cf\u6e05\u5355'}</span>

              <h2 className="mt-3 text-lg font-semibold text-slate-900">我的收藏</h2>

              <p className="mt-1 text-sm text-slate-500">收藏的路线会沉淀在这里，方便随时回看。</p>

            </div>

            <span className="chip">{collections.length} 条</span>

          </div>

          {collections.length ? (

            <div className="space-y-4">

              {collections.map((item) => (

                <div key={item.id} className="metric-card">

                  <div className="flex items-start justify-between gap-3">

                    <div>

                      <div className="text-sm font-medium text-slate-900">{item.routeTitle || '未命名路线'}</div>

                      <div className="mt-2 flex flex-wrap gap-2 text-xs text-slate-500">

                        {item.routeDurationDays ? <span>{item.routeDurationDays} 天</span> : null}

                        {item.routeDifficulty ? <span>{item.routeDifficulty}</span> : null}

                        {item.collectionTime ? <span>{item.collectionTime.slice(0, 10)}</span> : null}

                      </div>

                      {item.notes ? <div className="mt-2 text-sm leading-6 text-slate-500">{truncateText(item.notes)}</div> : null}

                    </div>

                    <button

                      type="button"

                      onClick={() => handleRemoveCollection(item)}

                      className="text-xs font-medium text-red-500 transition hover:text-red-600"

                    >

                      移除

                    </button>

                  </div>

                </div>

              ))}

            </div>

          ) : (

            <div className="py-8 text-center text-sm text-slate-400">还没有收藏内容</div>

          )}

        </div>



        <div className="scenic-shell-soft p-6">

          <div className="mb-4 flex items-center justify-between gap-3">

            <div>

              <span className="section-kicker">{'\u6211\u7684\u8def\u7ebf'}</span>

              <h2 className="mt-3 text-lg font-semibold text-slate-900">我的路线</h2>

              <p className="mt-1 text-sm text-slate-500">已创建的路线会在这里持续沉淀和复用。</p>

            </div>

            <span className="chip">{myRoutes.length} 条</span>

          </div>

          {myRoutes.length ? (

            <div className="space-y-4">

              {myRoutes.map((item) => (

                <div key={item.id} className="metric-card">

                  <div className="text-sm font-medium text-slate-900">{item.title}</div>

                  <div className="mt-2 text-sm leading-6 text-slate-500">{truncateText(item.description)}</div>

                  <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-400">

                    <span className="chip">时长 {item.durationDays || '--'} 天</span>

                    <span className="chip">浏览 {item.viewCount || 0}</span>

                    <span className="chip">点赞 {item.likeCount || 0}</span>

                  </div>

                </div>

              ))}

            </div>

          ) : (

            <div className="py-8 text-center text-sm text-slate-400">还没有创建路线</div>

          )}

        </div>

      </section>



      <section className="scenic-shell-soft p-6">

        <div className="mb-4 flex items-center justify-between gap-3">

          <div>

            <span className="section-kicker">旅行笔记</span>

            <h2 className="mt-3 text-lg font-semibold text-slate-900">我的游记</h2>

            <p className="mt-1 text-sm text-slate-500">记录下来的灵感和经历，方便继续完善与分享。</p>

          </div>

          <span className="chip">{myNotes.length} 篇</span>

        </div>

        {myNotes.length ? (

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">

            {myNotes.map((item) => (

              <article key={item.id} className="metric-card">

                <div className="text-sm font-medium text-slate-900">{item.title}</div>

                <p className="mt-2 line-clamp-4 text-sm leading-6 text-slate-500">

                  {truncateText(item.excerpt || item.content || TEXT.NO_CONTENT)}

                </p>

                <div className="mt-3 flex gap-3 text-xs text-slate-400">

                  <span>点赞 {item.likes || 0}</span>


                </div>

              </article>

            ))}

          </div>

        ) : (

          <div className="py-8 text-center text-sm text-slate-400">还没有发布游记</div>

        )}

      </section>

    </div>

  )

}

