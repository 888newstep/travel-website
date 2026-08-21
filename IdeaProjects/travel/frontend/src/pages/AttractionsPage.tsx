import { useCallback, useMemo, useRef, useState } from 'react'

import { attractionApi, type Attraction } from '../api/attraction.api'

import { CommentComposer, CommentFeed } from '../components/common/CommentBlocks'

import { DetailDrawer } from '../components/common/DetailDrawer'

import { DetailMetricsGrid, DetailTextCard } from '../components/common/DetailInfoBlocks'

import { LoadingSpinner } from '../components/common/LoadingSpinner'

import { SearchEmptyState } from '../components/common/SearchFeedback'

import { type StatusNoticeTone, StatusNotice } from '../components/common/StatusNotice'

import { DEBOUNCE_DELAY, TEXT } from '../constants'

import { getStoredToken } from '../lib/auth'

import { isRequestTimeoutError, withRequestTimeout } from '../lib/request'

import { useDebouncedKeywordEffect } from '../hooks/useDebouncedKeywordEffect'

import { useSyncedKeyword } from '../hooks/useSyncedKeyword'



function getFirstImage(item: Attraction) {

  if (!item.images || !Array.isArray(item.images)) {

    return ''

  }



  return item.images[0] || ''

}



function formatReviewTime(value?: string) {

  if (!value) return '未知'

  return value.replace('T', ' ').slice(0, 16)

}



interface InlineNotice {

  tone: StatusNoticeTone

  message: string

}



export function AttractionsPage() {

  const [items, setItems] = useState<Attraction[]>([])

  const [loading, setLoading] = useState(true)

  const { keyword, setKeyword, normalizedKeyword } = useSyncedKeyword()

  const [searching, setSearching] = useState(false)

  const [selectedItem, setSelectedItem] = useState<Attraction | null>(null)

  const [detailLoading, setDetailLoading] = useState(false)

  const [detailData, setDetailData] = useState<Record<string, any> | null>(null)

  const [detailImages, setDetailImages] = useState<string[]>([])

  const [reviews, setReviews] = useState<Record<string, any>[]>([])

  const [nearby, setNearby] = useState<Record<string, any>[]>([])

  const [reviewForm, setReviewForm] = useState({ rating: '5', content: '' })

  const [submittingReview, setSubmittingReview] = useState(false)

  const [searchNotice, setSearchNotice] = useState<InlineNotice | null>(null)

  const [detailNotice, setDetailNotice] = useState<InlineNotice | null>(null)

  const [reviewNotice, setReviewNotice] = useState<InlineNotice | null>(null)

  const hasFetchedRef = useRef(false)



  const hasToken = Boolean(getStoredToken())
  const fetchAttractions = useCallback(async (searchText: string) => {

    const normalized = searchText.trim()

    setSearchNotice(null)



    if (!hasFetchedRef.current) {

      setLoading(true)

    } else {

      setSearching(true)

    }



    try {

      const data = normalized

        ? await withRequestTimeout(attractionApi.searchAttractions(normalized))

        : await withRequestTimeout(attractionApi.getAttractions())

      setItems(Array.isArray(data) ? data : [])

    } catch (error) {

      setItems([])

      setSearchNotice({

        tone: normalized ? 'warning' : 'error',

        message: isRequestTimeoutError(error)

          ? '景点数据加载超时，请稍后重试。'

          : normalized

            ? '景点搜索失败，请更换关键词后重试。'

            : '景点列表加载失败，请稍后重试。',

      })

    } finally {

      setLoading(false)

      setSearching(false)

      hasFetchedRef.current = true

    }

  }, [])



  useDebouncedKeywordEffect({

    keyword: normalizedKeyword,

    delay: DEBOUNCE_DELAY,

    onKeywordChange: fetchAttractions,

  })



  const highRatedCount = useMemo(

    () => items.filter((item) => Number(item.rating) >= 4.5).length,

    [items],

  )



  const reviewFeedItems = useMemo(

    () => reviews.map((review, index) => ({

      id: String(review.id || `review-${index}`),

      author: String(review.username || review.userName || review.nickname || '游客'),

      time: formatReviewTime(review.createTime || review.createdAt),

      meta: `评分 ${review.rating || '--'} 分`,

      content: String(review.content || review.comment || '暂无点评内容'),

    })),

    [reviews],

  )



  const attractionMetricItems = useMemo(

    () => [

      { label: '评分', value: detailData?.rating || selectedItem?.rating || TEXT.NO_RATING },

      { label: '开放时间', value: detailData?.openingHours || selectedItem?.openingHours || '未提供' },

      { label: '联系方式', value: detailData?.contactInfo || selectedItem?.contactInfo || '未提供' },

      { label: '参考价格', value: detailData?.price || selectedItem?.price ? `¥${detailData?.price || selectedItem?.price}` : '未提供' },

    ],

    [detailData, selectedItem],

  )



  async function openDetail(item: Attraction) {

    setSelectedItem(item)

    setDetailLoading(true)

    setDetailNotice(null)

    setReviewNotice(null)

    setReviewForm({ rating: '5', content: '' })



    try {

      const [detailResult, imageResult, reviewResult, nearbyResult] = await Promise.allSettled([

        withRequestTimeout(attractionApi.getAttractionDetail(item.id!)),

        withRequestTimeout(attractionApi.getAttractionImages(item.id!)),

        withRequestTimeout(attractionApi.getAttractionReviews(item.id!)),

        withRequestTimeout(attractionApi.getAttractionNearby(item.id!)),

      ])



      setDetailData(detailResult.status === 'fulfilled' ? (detailResult.value as Record<string, any>) || null : null)

      setDetailImages(imageResult.status === 'fulfilled' ? (imageResult.value as string[]) || [] : [])

      setReviews(reviewResult.status === 'fulfilled' ? (reviewResult.value as Record<string, any>[]) || [] : [])

      setNearby(nearbyResult.status === 'fulfilled' ? (nearbyResult.value as Record<string, any>[]) || [] : [])

    } catch {

      setDetailData(null)

      setDetailImages([])

      setReviews([])

      setNearby([])

      setDetailNotice({ tone: 'error', message: '景点详情加载失败，请稍后重试。' })

    } finally {

      setDetailLoading(false)

    }

  }



  async function retryDetail() {

    if (!selectedItem) return

    setDetailLoading(true)

    setDetailNotice(null)



    try {

      const [detailResult, imageResult, reviewResult, nearbyResult] = await Promise.allSettled([

        withRequestTimeout(attractionApi.getAttractionDetail(selectedItem.id!)),

        withRequestTimeout(attractionApi.getAttractionImages(selectedItem.id!)),

        withRequestTimeout(attractionApi.getAttractionReviews(selectedItem.id!)),

        withRequestTimeout(attractionApi.getAttractionNearby(selectedItem.id!)),

      ])



      setDetailData(detailResult.status === 'fulfilled' ? (detailResult.value as Record<string, any>) || null : null)

      setDetailImages(imageResult.status === 'fulfilled' ? (imageResult.value as string[]) || [] : [])

      setReviews(reviewResult.status === 'fulfilled' ? (reviewResult.value as Record<string, any>[]) || [] : [])

      setNearby(nearbyResult.status === 'fulfilled' ? (nearbyResult.value as Record<string, any>[]) || [] : [])

    } catch {

      setDetailData(null)

      setDetailImages([])

      setReviews([])

      setNearby([])

      setDetailNotice({ tone: 'error', message: '景点详情刷新失败，请稍后再试。' })

    } finally {

      setDetailLoading(false)

    }

  }



  async function submitReview() {

    if (!selectedItem || !reviewForm.content.trim()) return



    setSubmittingReview(true)

    setReviewNotice(null)



    try {

      await attractionApi.submitReview(selectedItem.id!, Number(reviewForm.rating), reviewForm.content.trim())

      setReviewForm({ rating: '5', content: '' })

      const reviewResult = await attractionApi.getAttractionReviews(selectedItem.id!)

      setReviews(Array.isArray(reviewResult) ? reviewResult : [])

    } catch {

      setReviewNotice({ tone: 'error', message: '点评提交失败，请稍后再试。' })

    } finally {

      setSubmittingReview(false)

    }

  }



  function retrySearch() {

    fetchAttractions(normalizedKeyword)

  }



  return (

    <div className="app-container pb-16 pt-4 md:pt-6">

      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">

        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />

        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />



        <div className="relative grid gap-6 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">

          <div>

            <span className="section-kicker">景点探索</span>

            <div className="mt-3 flex flex-wrap gap-2">

              <span className="chip">城市地标</span>

              <span className="chip">出游灵感</span>

              <span className="chip">热门打卡</span>

            </div>

            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">发现更值得去的景点</h1>

            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">

              从评分、开放时间、门票信息与周边推荐中快速筛选灵感，先找到想去的地方，再决定整段旅程如何展开。

            </p>

            <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">

              <div className="metric-card">

                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">景点总数</div>

                <div className="mt-3 text-2xl font-semibold text-slate-900">{items.length}</div>

              </div>

              <div className="metric-card">

                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">当前状态</div>

                <div className="mt-3 text-base font-semibold text-slate-900">{normalizedKeyword ? '关键词筛选' : '全部景点'}</div>

              </div>

              <div className="metric-card">

                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">高分景点</div>

                <div className="mt-3 text-2xl font-semibold text-slate-900">{highRatedCount}</div>

              </div>

              <div className="metric-card">

                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">周边推荐</div>

                <div className="mt-3 text-2xl font-semibold text-slate-900">{nearby.length}</div>

              </div>

            </div>

          </div>



          <div className="scenic-shell-soft edge-glow animate-fade-in p-5 sm:p-6">

            <div className="mb-4 flex items-center justify-between">

              <div>

                <div className="text-sm font-medium text-slate-500">快速搜索</div>

                <div className="mt-1 text-xl font-semibold text-slate-900">先找景点，再安排顺路玩法</div>

              </div>

              <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">实时筛选</span>

            </div>

            <input

              value={keyword}

              onChange={(event) => setKeyword(event.target.value)}

              type="text"

              placeholder="搜索景点名称、地址或关键词"

              className="search-input"

            />

            {searchNotice ? (

              <div className="mt-4">

                <StatusNotice tone={searchNotice.tone} message={searchNotice.message} actionLabel="重试" onAction={retrySearch} />

              </div>

            ) : null}

          </div>

        </div>

      </section>



      {searching ? (

        <div className="mt-8">

          <LoadingSpinner />

        </div>

      ) : null}



      {!loading && !items.length ? (

        <SearchEmptyState message="暂未找到符合条件的景点。" actionLabel="重新加载" onAction={retrySearch} />

      ) : null}



      {!loading && items.length ? (

        <section className="mt-8 grid gap-5 md:grid-cols-2 xl:grid-cols-3">

          {items.map((item) => (

            <article key={item.id} className="scenic-shell-soft surface-card-hover overflow-hidden">

              {getFirstImage(item) ? (

                <img src={getFirstImage(item)} alt={item.name} className="h-44 w-full object-cover" />

              ) : (

                <div className="h-44 bg-gradient-to-br from-sky-100 via-white to-emerald-100" />

              )}

              <div className="p-5">

                <div className="mb-3 flex items-center justify-between gap-3">

                  <h2 className="text-lg font-semibold text-slate-900">{item.name}</h2>

                  <span className="rounded-full bg-sky-100 px-2.5 py-1 text-xs font-medium text-slate-500">

                    {item.rating ? `⭐${item.rating}` : TEXT.NO_RATING}

                  </span>

                </div>

                <p className="line-clamp-3 text-sm leading-6 text-slate-500">{item.description || TEXT.NO_DESCRIPTION}</p>

                <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-400">

                  {item.address ? <span className="chip">{item.address}</span> : null}

                  {item.openingHours ? <span className="chip">{item.openingHours}</span> : null}

                  {item.price ? <span className="chip">¥{item.price}</span> : null}

                </div>

                <button type="button" onClick={() => openDetail(item)} className="btn-secondary mt-4 w-full">

                  查看详情

                </button>

              </div>

            </article>

          ))}

        </section>

      ) : null}



      <DetailDrawer

        open={Boolean(selectedItem)}

        title={selectedItem?.name || '景点详情'}

        subtitle={selectedItem?.address || '暂无地址信息'}

        loading={detailLoading}

        onClose={() => {

          setSelectedItem(null)

          setDetailData(null)

          setDetailImages([])

          setReviews([])

          setNearby([])

          setDetailNotice(null)

          setReviewNotice(null)

        }}

      >

        <div className="space-y-6">

          {detailNotice ? (

            <StatusNotice tone={detailNotice.tone} message={detailNotice.message} actionLabel="重试" onAction={retryDetail} />

          ) : null}



          {detailImages.length ? (

            <div className="grid gap-3 sm:grid-cols-2">

              {detailImages.slice(0, 4).map((image, index) => (

                <img key={`${image}-${index}`} src={image} alt={`${selectedItem?.name || '景点'}-${index + 1}`} className="h-40 w-full rounded-2xl object-cover" />

              ))}

            </div>

          ) : null}



          <div className="grid gap-3 sm:grid-cols-2">

            <DetailTextCard title="景点简介">

              {detailData?.description || selectedItem?.description || TEXT.NO_DESCRIPTION}

            </DetailTextCard>

            <DetailMetricsGrid items={attractionMetricItems} columnsClassName="sm:grid-cols-1" />

          </div>



          <CommentFeed title="游客点评" items={reviewFeedItems} emptyText="还没有点评，欢迎留下第一条体验。" />



          {reviewNotice ? (

            <StatusNotice

              tone={reviewNotice.tone}

              message={reviewNotice.message}

               actionLabel={reviewNotice.tone === 'error' ? '重试' : undefined}

              onAction={reviewNotice.tone === 'error' ? submitReview : undefined}

            />

          ) : null}



          {hasToken ? (

            <CommentComposer

              title="发布点评"

              value={reviewForm.content}

              onChange={(value) => setReviewForm((current) => ({ ...current, content: value }))}

              onSubmit={submitReview}

              submitting={submittingReview}

              disabled={submittingReview}

              placeholder="写下你的游玩体验或避坑建议"

              submitText="提交点评"

              submittingText="提交中..."

              ratingValue={reviewForm.rating}

              onRatingChange={(value) => setReviewForm((current) => ({ ...current, rating: value }))}

            />

          ) : null}



          <div>

            <h3 className="text-base font-semibold text-slate-900">周边推荐</h3>

            {nearby.length ? (

              <div className="mt-3 grid gap-3 sm:grid-cols-2">

                {nearby.slice(0, 4).map((item, index) => (

                  <div key={item.id || index} className="metric-card">

                    <div className="font-medium text-slate-900">{item.name || `周边景点编号 ${index + 1}`}</div>

                    <div className="mt-2 line-clamp-2 text-sm text-slate-500">{item.description || item.address || '暂无补充说明'}</div>

                    {typeof item.distance === 'number' ? (
                      <div className="mt-1 text-xs text-slate-400">距离约 {item.distance.toFixed(1)} 公里</div>
                    ) : null}

                  </div>

                ))}

              </div>

            ) : (

               <div className="mt-3 rounded-2xl border border-dashed border-slate-200 px-4 py-4 text-sm text-slate-400">暂无周边推荐</div>

            )}

          </div>

        </div>

      </DetailDrawer>

    </div>

  )

}
