import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { collectionApi } from '../api/collection.api'
import { commentApi, type RouteComment } from '../api/comment.api'
import { intelligentRouteApi, routeCrudApi, type Route, type RoutePlan } from '../api/route.api'
import { userApi } from '../api/user.api'
import { CommentComposer, CommentFeed } from '../components/common/CommentBlocks'
import { DetailDrawer } from '../components/common/DetailDrawer'
import { DetailMetricsGrid, DetailTextCard } from '../components/common/DetailInfoBlocks'
import { RelatedCard, RelatedSection } from '../components/common/RelatedBlocks'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { SearchEmptyState } from '../components/common/SearchFeedback'
import { StatCard, StatsGrid } from '../components/common/PageBlocks'
import { type StatusNoticeTone, StatusNotice } from '../components/common/StatusNotice'
import { DEBOUNCE_DELAY, DEFAULT_CITY_ID } from '../constants'
import { getStoredToken } from '../lib/auth'
import { isRequestTimeoutError, withRequestTimeout } from '../lib/request'
import { useDebouncedKeywordEffect } from '../hooks/useDebouncedKeywordEffect'
import { useSyncedKeyword } from '../hooks/useSyncedKeyword'

type RouteTab = 'local' | 'popular'
type DisplayRoute = Route | RoutePlan

interface InlineNotice {
  tone: StatusNoticeTone
  message: string
}

function formatPreferenceTags(preferences?: Record<string, unknown>) {
  if (!preferences) {
    return [] as string[]
  }

  return Object.entries(preferences)
    .slice(0, 3)
    .map(([key, value]) => `${key}: ${String(value)}`)
}

function getDifficultyOptions(routes: Route[]) {
  return Array.from(new Set(routes.map((item) => item.difficulty).filter((item): item is string => Boolean(item))))
}

function getRouteId(route?: DisplayRoute | null) {
  return typeof route?.id === 'number' ? route.id : null
}

function getRouteDescription(route?: DisplayRoute | null) {
  if (!route) {
    return '暂未获取到路线详情，稍后可重新查看。'
  }

  if ('description' in route && route.description) {
    return route.description
  }

  return '这是一条由 AI 生成的推荐路线，建议结合个人时间与预算进一步调整。'
}

function getRouteDifficulty(route?: DisplayRoute | null) {
  if (route && 'difficulty' in route && route.difficulty) {
    return route.difficulty
  }

  return '待定'
}

function getRouteCover(route?: DisplayRoute | null) {
  if (route && 'coverImage' in route && route.coverImage) {
    return route.coverImage
  }

  return ''
}

function getRouteTags(route?: DisplayRoute | null) {
  if (!route || !('preferences' in route)) {
    return [] as string[]
  }

  return formatPreferenceTags(route.preferences)
}

function getRouteDuration(route?: DisplayRoute | null) {
  return route?.durationDays ? `${route.durationDays} 天` : '未设置'
}

function formatTime(value?: string) {
  if (!value) return '未知'
  return value.replace('T', ' ').slice(0, 16)
}

function getCommentCount(stats: Record<string, unknown> | null, comments: RouteComment[]) {
  const value = stats?.totalComments
  if (typeof value === 'number') {
    return value
  }

  if (typeof value === 'string') {
    const count = Number(value)
    return Number.isFinite(count) ? count : comments.length
  }

  return comments.length
}

function getAverageRating(stats: Record<string, unknown> | null, comments: RouteComment[]) {
  const value = stats?.averageRating
  if (typeof value === 'number') {
    return value.toFixed(1)
  }

  if (typeof value === 'string') {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) {
      return parsed.toFixed(1)
    }
  }

  const ratedComments = comments.filter((item) => typeof item.rating === 'number')
  if (!ratedComments.length) {
    return '--'
  }

  const total = ratedComments.reduce((sum, item) => sum + (item.rating || 0), 0)
  return (total / ratedComments.length).toFixed(1)
}

export function RoutesPage() {
  const [tab, setTab] = useState<RouteTab>('local')
  const [localRoutes, setLocalRoutes] = useState<Route[]>([])
  const [searchedLocalRoutes, setSearchedLocalRoutes] = useState<Route[]>([])
  const [popularRoutes, setPopularRoutes] = useState<RoutePlan[]>([])
  const [loading, setLoading] = useState(true)
  const [searchingLocal, setSearchingLocal] = useState(false)
  const { keyword, setKeyword, normalizedKeyword } = useSyncedKeyword()
  const [activeDifficulty, setActiveDifficulty] = useState('全部')
  const [selectedRoute, setSelectedRoute] = useState<DisplayRoute | null>(null)
  const [detailMode, setDetailMode] = useState<RouteTab | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailData, setDetailData] = useState<DisplayRoute | null>(null)
  const [routeComments, setRouteComments] = useState<RouteComment[]>([])
  const [similarRoutes, setSimilarRoutes] = useState<RoutePlan[]>([])
  const [commentStats, setCommentStats] = useState<Record<string, unknown> | null>(null)
  const [commentForm, setCommentForm] = useState({ rating: '5', content: '' })
  const [commentSubmitting, setCommentSubmitting] = useState(false)
  const [currentUserId, setCurrentUserId] = useState<number | null>(null)
  const [collected, setCollected] = useState(false)
  const [collectionSubmitting, setCollectionSubmitting] = useState(false)
  const [pageNotice, setPageNotice] = useState<InlineNotice | null>(null)
  const [drawerNotice, setDrawerNotice] = useState<InlineNotice | null>(null)
  const hasToken = Boolean(getStoredToken())
  const searchRequestRef = useRef(0)

  const loadOverviewData = useCallback(async () => {
    setPageNotice(null)
    setLoading(true)

    const [localResult, popularResult] = await Promise.allSettled([
      withRequestTimeout(routeCrudApi.getRoutesByCity(DEFAULT_CITY_ID)),
      withRequestTimeout(intelligentRouteApi.getPopularRoutes(DEFAULT_CITY_ID, 3, 6)),
    ])

    setLocalRoutes(localResult.status === 'fulfilled' && Array.isArray(localResult.value) ? localResult.value : [])
    setPopularRoutes(popularResult.status === 'fulfilled' && Array.isArray(popularResult.value) ? popularResult.value : [])

    const hasTimeout = [localResult, popularResult].some(
      (result) => result.status === 'rejected' && isRequestTimeoutError(result.reason),
    )

    if (localResult.status === 'rejected' && popularResult.status === 'rejected') {
      setPageNotice({
        tone: 'error',
        message: hasTimeout ? '路线数据加载超时，请稍后重试。' : '路线数据加载失败，请稍后重试。',
      })
    } else if (localResult.status === 'rejected' || popularResult.status === 'rejected') {
      setPageNotice({
        tone: 'warning',
        message: hasTimeout ? '部分路线加载超时，已先展示可用内容。' : '部分路线加载失败，已先展示当前可用内容。',
      })
    }

    setLoading(false)
  }, [])

  useEffect(() => {
    void loadOverviewData()
  }, [loadOverviewData])

  const runLocalSearch = useCallback(async (searchText: string) => {
    setPageNotice(null)
    if (!searchText) {
      setSearchedLocalRoutes([])
      setSearchingLocal(false)
      return
    }

    setSearchingLocal(true)
    const requestId = searchRequestRef.current + 1
    searchRequestRef.current = requestId

    try {
      const result = await withRequestTimeout(routeCrudApi.searchRoutes(searchText))
      if (searchRequestRef.current !== requestId) {
        return
      }

      setSearchedLocalRoutes(Array.isArray(result) ? result : [])
    } catch (error) {
      if (searchRequestRef.current === requestId) {
        setSearchedLocalRoutes([])
        setPageNotice({
          tone: 'warning',
          message: isRequestTimeoutError(error) ? '搜索响应超时，请稍后重试。' : '本地路线搜索失败，请更换关键词后重试。',
        })
      }
    } finally {
      if (searchRequestRef.current === requestId) {
        setSearchingLocal(false)
      }
    }
  }, [])

  useDebouncedKeywordEffect({
    keyword: normalizedKeyword,
    delay: DEBOUNCE_DELAY,
    onKeywordChange: runLocalSearch,
  })

  useEffect(() => {
    let active = true

    if (!hasToken) {
      setCurrentUserId(null)
      return () => {
        active = false
      }
    }

    userApi.getCurrentUser()
      .then((user) => {
        if (active && user?.id) {
          setCurrentUserId(user.id)
        }
      })
      .catch(() => {
        if (active) {
          setCurrentUserId(null)
        }
      })

    return () => {
      active = false
    }
  }, [hasToken])

  const localRouteSource = normalizedKeyword ? searchedLocalRoutes : localRoutes
  const routeCommentFeedItems = useMemo(
    () => routeComments.map((comment, index) => ({
      id: String(comment.id || `route-comment-${index}`),
      author: comment.isAnonymous ? '匿名用户' : `用户编号 ${comment.userId}`,
      time: formatTime(comment.createTime || comment.updateTime),
      meta: `评分 ${comment.rating || '--'} 分 · 点赞 ${comment.likeCount || 0}`,
      content: String(comment.content || '暂无评论内容'),
    })),
    [routeComments],
  )

  const difficultyOptions = useMemo(() => ['全部', ...getDifficultyOptions(localRouteSource)], [localRouteSource])

  const filteredLocalRoutes = useMemo(() => {
    return localRouteSource.filter((item) => activeDifficulty === '全部' || item.difficulty === activeDifficulty)
  }, [activeDifficulty, localRouteSource])

  const filteredPopularRoutes = useMemo(() => {
    const text = normalizedKeyword.toLowerCase()
    if (!text) {
      return popularRoutes
    }

    return popularRoutes.filter((item) => {
      const preferenceText = item.preferences ? JSON.stringify(item.preferences).toLowerCase() : ''
      return [item.title, preferenceText].some((value) => String(value).toLowerCase().includes(text))
    })
  }, [normalizedKeyword, popularRoutes])

  const activeList = tab === 'local' ? filteredLocalRoutes : filteredPopularRoutes
  const activeSummary = tab === 'local' ? '本地路线' : '热门推荐'
  const activeDetail = detailData || selectedRoute
  const activeRouteId = getRouteId(activeDetail)

  const routeMetricItems = useMemo(
    () => [
      { label: '行程时长', value: getRouteDuration(activeDetail) },
      { label: '难度等级', value: getRouteDifficulty(activeDetail) },
      { label: '评论数量', value: getCommentCount(commentStats, routeComments) },
      { label: '综合评分', value: getAverageRating(commentStats, routeComments) },
    ],
    [activeDetail, commentStats, routeComments],
  )

  async function ensureCurrentUser() {
    if (currentUserId) {
      return currentUserId
    }

    if (!getStoredToken()) {
      return null
    }

    try {
      const user = await userApi.getCurrentUser()
      if (user?.id) {
        setCurrentUserId(user.id)
        return user.id
      }
    } catch {
      setCurrentUserId(null)
    }

    return null
  }

  function resetDetailState() {
    setDetailData(null)
    setRouteComments([])
    setSimilarRoutes([])
    setCommentStats(null)
    setCommentForm({ rating: '5', content: '' })
    setCollected(false)
  }

  async function openLocalDetail(route: Route) {
    setSelectedRoute(route)
    setDetailMode('local')
    setDetailLoading(true)
    resetDetailState()
    setDrawerNotice(null)

    if (!route.id) {
      setDetailData(route)
      setDetailLoading(false)
      return
    }

    try {
      const [detailResult, commentResult, similarResult, statsResult] = await Promise.allSettled([
        withRequestTimeout(routeCrudApi.getRoute(route.id)),
        withRequestTimeout(commentApi.getLatestComments(route.id, 6)).catch(() => withRequestTimeout(commentApi.getRouteComments(route.id, 1, 6))),
        withRequestTimeout(intelligentRouteApi.getSimilarRoutes(route.id, 4)),
        withRequestTimeout(commentApi.getCommentStatistics(route.id)),
      ])

      setDetailData(detailResult.status === 'fulfilled' && detailResult.value ? detailResult.value : route)
      setRouteComments(commentResult.status === 'fulfilled' && Array.isArray(commentResult.value) ? commentResult.value : [])
      setSimilarRoutes(similarResult.status === 'fulfilled' && Array.isArray(similarResult.value) ? similarResult.value : [])
      setCommentStats(statsResult.status === 'fulfilled' && statsResult.value ? statsResult.value : null)

      const userId = await ensureCurrentUser()
      if (userId) {
        try {
          const value = await collectionApi.checkCollected(route.id)
          setCollected(Boolean(value))
        } catch {
          setCollected(false)
        }
      }
    } catch (error) {
      setDetailData(route)
      setRouteComments([])
      setSimilarRoutes([])
      setCommentStats(null)
      setDrawerNotice({
        tone: 'error',
        message: isRequestTimeoutError(error) ? '路线详情加载超时，请稍后重试。' : '路线详情加载失败，请稍后重试。',
      })
    } finally {
      setDetailLoading(false)
    }
  }

  async function openPopularDetail(route: RoutePlan) {
    setSelectedRoute(route)
    setDetailMode('popular')
    setDetailLoading(true)
    resetDetailState()
    setDetailData(route)
    setDrawerNotice(null)

    const routeId = getRouteId(route)
    if (!routeId) {
      setDetailLoading(false)
      return
    }

    try {
      const similar = await withRequestTimeout(intelligentRouteApi.getSimilarRoutes(routeId, 4))
      setSimilarRoutes(Array.isArray(similar) ? similar : [])
    } catch (error) {
      setSimilarRoutes([])
      setDrawerNotice({
        tone: 'warning',
        message: isRequestTimeoutError(error) ? '相似路线加载超时，请稍后重试。' : '相似路线加载失败，稍后再试。',
      })
    } finally {
      setDetailLoading(false)
    }
  }

  async function handleToggleCollection() {
    if (!activeRouteId || detailMode !== 'local' || collectionSubmitting) {
      return
    }

    const userId = await ensureCurrentUser()
    if (!userId) {
      return
    }

    setCollectionSubmitting(true)
    setDrawerNotice(null)
    try {
      const result = await withRequestTimeout(collectionApi.toggleCollection(activeRouteId))
      setCollected(Boolean(result?.collected))
      setDrawerNotice({
        tone: 'success',
        message: result?.collected ? '已加入收藏。' : '已取消收藏。',
      })
    } catch (error) {
      setDrawerNotice({
        tone: 'error',
        message: isRequestTimeoutError(error) ? '收藏操作超时，请稍后再试。' : '收藏操作失败，请稍后重试。',
      })
    } finally {
      setCollectionSubmitting(false)
    }
  }

  async function submitComment() {
    if (!activeRouteId || detailMode !== 'local' || !commentForm.content.trim() || commentSubmitting) {
      return
    }

    const userId = await ensureCurrentUser()
    if (!userId) {
      return
    }

    setCommentSubmitting(true)
    setDrawerNotice(null)
    try {
      await withRequestTimeout(commentApi.createComment({
        routeId: activeRouteId,
        userId,
        rating: Number(commentForm.rating) || 5,
        content: commentForm.content.trim(),
        isAnonymous: false,
      }))

      const [commentResult, statsResult] = await Promise.allSettled([
        withRequestTimeout(commentApi.getLatestComments(activeRouteId, 6)).catch(() => withRequestTimeout(commentApi.getRouteComments(activeRouteId, 1, 6))),
        withRequestTimeout(commentApi.getCommentStatistics(activeRouteId)),
      ])

      setRouteComments(commentResult.status === 'fulfilled' && Array.isArray(commentResult.value) ? commentResult.value : [])
      setCommentStats(statsResult.status === 'fulfilled' && statsResult.value ? statsResult.value : null)
      setCommentForm({ rating: '5', content: '' })
      setDrawerNotice({ tone: 'success', message: '评论发布成功。' })
    } catch (error) {
      setDrawerNotice({
        tone: 'error',
        message: isRequestTimeoutError(error) ? '评论提交超时，请稍后再试。' : '评论提交失败，请稍后重试。',
      })
    } finally {
      setCommentSubmitting(false)
    }
  }

  function retryPageData() {
    if (normalizedKeyword && tab === 'local') {
      void runLocalSearch(normalizedKeyword)
      return
    }

    void loadOverviewData()
  }

  function retryDrawerData() {
    if (!selectedRoute || !detailMode) {
      return
    }

    if (detailMode === 'local') {
      void openLocalDetail(selectedRoute as Route)
      return
    }

    void openPopularDetail(selectedRoute as RoutePlan)
  }

  function closeDrawer() {
    setSelectedRoute(null)
    setDetailMode(null)
    setDetailLoading(false)
    resetDetailState()
    setDrawerNotice(null)
  }

  const drawerFooter = detailMode === 'local' ? (
    <div className="flex flex-wrap gap-3">
      <button
        type="button"
        onClick={handleToggleCollection}
        disabled={collectionSubmitting || !hasToken || !activeRouteId}
        className="btn-primary disabled:cursor-not-allowed disabled:opacity-60"
      >
        {collectionSubmitting ? '处理中...' : collected ? '取消收藏' : '加入收藏'}
      </button>
      <Link to="/ai-chat" className="btn-secondary">
        咨询 AI 助手
      </Link>
    </div>
  ) : (
    <div className="flex flex-wrap gap-3">
      <Link to="/ai-chat" className="btn-primary">
        让 AI 解析路线
      </Link>
      <Link to="/optimization" className="btn-secondary">
        去优化路线
      </Link>
    </div>
  )

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

        <div className="relative grid gap-8 xl:grid-cols-[1.08fr_0.92fr] xl:items-end">
          <div>
            <span className="section-kicker">{'\u8def\u7ebf\u4e2d\u5fc3'}</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">城市漫游</span>
              <span className="chip">热门推荐</span>
              <span className="chip">轻松规划</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">发现更适合你的旅行路线</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              在本地路线库与 AI 热门推荐之间自由切换，结合天数、难度、偏好与评论信息，快速筛出更适合当前出游节奏的行程方案。
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link to="/ai-chat" className="btn-primary">
                让 AI 推荐路线
              </Link>
              <Link to="/optimization" className="btn-secondary">
                查看优化建议
              </Link>
            </div>
          </div>

          <div className="scenic-shell-soft p-5 sm:p-6">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <div className="text-sm font-medium text-slate-500">快速检索</div>
                <div className="mt-1 text-xl font-semibold text-slate-900">先搜路线，再决定怎么玩</div>
              </div>
              <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">实时同步</span>
            </div>
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              type="text"
              placeholder="搜索路线标题或关键词..."
              className="search-input"
            />
            <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-500">
              <span className="chip">当前分类：{activeSummary}</span>
              <span className="chip">匹配结果：{activeList.length}</span>
              {tab === 'local' && normalizedKeyword ? (
                <span className="chip">{searchingLocal ? '搜索中...' : '搜索完成'}</span>
              ) : null}
            </div>
          </div>
        </div>
      </section>

      {pageNotice ? (
        <StatusNotice
          tone={pageNotice.tone}
          message={pageNotice.message}
          actionLabel="重试"
          onAction={retryPageData}
          className="mt-8"
        />
      ) : null}

      <section className="surface-card edge-glow mb-8 mt-8 animate-fade-in rounded-[1.75rem] p-4 sm:p-5">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="section-heading text-[1.75rem]">路线总览</h2>
            <p className="section-subtitle mt-2">在本地路线与智能推荐之间切换，快速找到适合当前旅行节奏的方案。</p>
          </div>
          <span className="chip">当前显示：{activeSummary}</span>
        </div>

        <div className="flex gap-2 overflow-x-auto rounded-full border border-sky-200/70 bg-sky-50/90 p-2">
          <button
            type="button"
            onClick={() => setTab('local')}
            className={`tab-pill ${tab === 'local' ? 'tab-pill-active' : 'tab-pill-idle'}`}
          >
            本地路线
          </button>
          <button
            type="button"
            onClick={() => setTab('popular')}
            className={`tab-pill ${tab === 'popular' ? 'tab-pill-active' : 'tab-pill-idle'}`}
          >
            热门推荐
          </button>
        </div>

        {tab === 'local' ? (
          <div className="mt-4 flex flex-wrap gap-2">
            {difficultyOptions.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => setActiveDifficulty(item)}
                className={`rounded-full px-4 py-2 text-sm font-medium transition ${
                  activeDifficulty === item
                    ? 'bg-gradient-to-r from-sky-500 to-emerald-500 text-white shadow-md shadow-sky-500/20'
                    : 'bg-slate-50 text-slate-500 hover:bg-sky-50 hover:text-sky-700'
                }`}
              >
                {item}
              </button>
            ))}
          </div>
        ) : null}
      </section>

      <StatsGrid className="mb-8">
        <StatCard label="本地路线" value={localRoutes.length} hint="来自当前城市路线库" />
        <StatCard label="热门推荐" value={popularRoutes.length} hint="来自 AI 热门行程推荐" />
        <StatCard label="当前结果" value={activeList.length} hint="已按关键词与筛选条件整理" />
      </StatsGrid>

      {loading ? <LoadingSpinner /> : null}
      {!loading && searchingLocal && tab === 'local' ? <LoadingSpinner /> : null}

      {!loading && !activeList.length ? (
        <SearchEmptyState message="暂未找到符合条件的路线。" actionLabel="重新加载" onAction={retryPageData} />
      ) : null}

      {!loading && tab === 'local' && filteredLocalRoutes.length ? (
        <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {filteredLocalRoutes.map((item, index) => (
            <article
              key={item.id ?? `${item.title}-${index}`}
              className="surface-card surface-card-hover edge-glow animate-scale-in rounded-[1.75rem] p-5"
            >
              <div className="mb-3 flex items-center justify-between gap-3">
                <span className="rounded-full bg-sky-100 px-2.5 py-1 text-xs font-medium text-sky-700">本地路线</span>
                <span className="text-xs text-slate-400">{item.difficulty || '待定'}</span>
              </div>
              <h3 className="text-lg font-semibold text-slate-900">{item.title}</h3>
              <p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-500">
                {item.description || '暂无路线简介，进入详情后可查看更完整的行程信息。'}
              </p>
              <div className="mt-5 flex flex-wrap gap-2">
                <span className="chip">时长 {item.durationDays || '--'} 天</span>
                <span className="chip">浏览 {item.viewCount || 0}</span>
                <span className="chip">点赞 {item.likeCount || 0}</span>
              </div>
              <button type="button" onClick={() => openLocalDetail(item)} className="btn-secondary mt-4 w-full">
                查看详情
              </button>
            </article>
          ))}
        </section>
      ) : null}

      {!loading && tab === 'popular' && filteredPopularRoutes.length ? (
        <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {filteredPopularRoutes.map((item, index) => {
            const tags = formatPreferenceTags(item.preferences)

            return (
              <article
                key={item.id ?? `${item.title}-${index}`}
                className="surface-card surface-card-hover edge-glow animate-scale-in rounded-[1.75rem] p-5"
              >
                <div className="mb-3 flex items-center justify-between gap-3">
                  <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-600">AI 推荐</span>
                  <span className="text-xs text-slate-400">{item.durationDays || '--'} 天</span>
                </div>
                <h3 className="text-lg font-semibold text-slate-900">{item.title}</h3>
                <p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-500">
                  {tags.length ? `偏好标签：${tags.join(' · ')}` : '暂未提供偏好标签，稍后可让 AI 补充说明。'}
                </p>
                <button type="button" onClick={() => openPopularDetail(item)} className="btn-secondary mt-4 w-full">
                  查看详情
                </button>
              </article>
            )
          })}
        </section>
      ) : null}

      <DetailDrawer
        open={Boolean(selectedRoute && detailMode)}
        title={selectedRoute?.title || '路线详情'}
        subtitle={detailMode === 'local' ? '查看路线详情、评论和收藏状态' : '查看 AI 推荐路线与相似方案'}
        loading={detailLoading}
        footer={drawerFooter}
        onClose={closeDrawer}
      >
        <div className="space-y-6">
          {drawerNotice ? (
            <StatusNotice tone={drawerNotice.tone} message={drawerNotice.message} actionLabel="重试" onAction={retryDrawerData} />
          ) : null}

          {getRouteCover(activeDetail) ? (
            <img
              src={getRouteCover(activeDetail)}
              alt={selectedRoute?.title || '路线封面'}
              className="h-56 w-full rounded-3xl object-cover"
            />
          ) : null}

          <DetailMetricsGrid items={routeMetricItems} />

          <DetailTextCard>{getRouteDescription(activeDetail)}</DetailTextCard>

          {detailMode === 'popular' ? (
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-2xl bg-slate-50 px-4 py-4 text-sm text-slate-600">
                <div className="text-xs text-slate-500">偏好标签</div>
                <div className="mt-2 flex flex-wrap gap-2">
                  {getRouteTags(activeDetail).length ? (
                    getRouteTags(activeDetail).map((tag) => (
                      <span key={tag} className="chip">{tag}</span>
                    ))
                  ) : (
                    <span className="text-slate-400">暂未解析出偏好标签，可继续咨询 AI 助手。</span>
                  )}
                </div>
              </div>
              <div className="rounded-2xl bg-slate-50 px-4 py-4 text-sm text-slate-600">
                <div className="text-xs text-slate-500">行程建议</div>
                <div className="mt-2 leading-6">
                  建议结合出行日期、预算、同行人和实时天气，对这条推荐路线进行二次确认后再落地执行。
                </div>
              </div>
            </div>
          ) : null}

          {similarRoutes.length ? (
            <RelatedSection title="相似路线" hasItems={similarRoutes.length > 0} emptyText="暂无推荐">
              {similarRoutes.map((item, index) => {
                const tags = formatPreferenceTags(item.preferences)

                return (
                  <RelatedCard
                    key={String(item.id ?? `${item.title}-${index}`)}
                    title={item.title}
                    side={`${item.durationDays || '--'} 天`}
                    description={tags.length ? tags.join(' · ') : '暂未提供偏好摘要'}
                    onClick={() => openPopularDetail(item)}
                  />
                )
              })}
            </RelatedSection>
          ) : null}

          {detailMode === 'local' ? (
            <CommentFeed
              title="用户评论"
              items={routeCommentFeedItems}
              emptyText="这条路线还没有评论，欢迎留下第一条体验。"
            />
          ) : null}

          {detailMode === 'local' && hasToken ? (
            <CommentComposer
              title="发表评价"
              value={commentForm.content}
              onChange={(value) => setCommentForm((current) => ({ ...current, content: value }))}
              onSubmit={submitComment}
              submitting={commentSubmitting}
              disabled={commentSubmitting}
              placeholder="写下你的出行体验或建议..."
              submitText="提交评论"
              submittingText="提交中..."
              ratingValue={commentForm.rating}
              onRatingChange={(value) => setCommentForm((current) => ({ ...current, rating: value }))}
            />
          ) : null}

          {detailMode === 'local' && !hasToken ? (
            <div className="rounded-2xl border border-dashed border-slate-200 px-4 py-4 text-sm text-slate-500">
              登录后可收藏路线、发布评论，并同步你的个人行程。
            </div>
          ) : null}
        </div>
      </DetailDrawer>
    </div>
  )
}

