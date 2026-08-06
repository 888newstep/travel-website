import { useCallback, useMemo, useRef, useState } from 'react'
import { noteApi, type TravelNote } from '../api/note.api'
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

function formatTime(value?: string) {
  if (!value) return '未知'
  return value.replace('T', ' ').slice(0, 16)
}

interface InlineNotice { tone: StatusNoticeTone; message: string }

export function NotesPage() {
  const [notes, setNotes] = useState<TravelNote[]>([])
  const [hotNotes, setHotNotes] = useState<TravelNote[]>([])
  const [loading, setLoading] = useState(true)
  const { keyword, setKeyword, normalizedKeyword } = useSyncedKeyword()
  const [searching, setSearching] = useState(false)
  const [selectedNote, setSelectedNote] = useState<TravelNote | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detail, setDetail] = useState<Record<string, any> | null>(null)
  const [comments, setComments] = useState<Record<string, any>[]>([])
  const [commentInput, setCommentInput] = useState('')
  const [commentSubmitting, setCommentSubmitting] = useState(false)
  const [searchNotice, setSearchNotice] = useState<InlineNotice | null>(null)
  const [detailNotice, setDetailNotice] = useState<InlineNotice | null>(null)
  const [commentNotice, setCommentNotice] = useState<InlineNotice | null>(null)
  const hasFetchedRef = useRef(false)
  const hasToken = Boolean(getStoredToken())

  const loadNotes = useCallback(async (searchText: string) => {
    const text = searchText.trim()
    setSearchNotice(null)
    if (!hasFetchedRef.current) setLoading(true)
    else setSearching(true)
    try {
      const [list, hot] = await Promise.all([
        withRequestTimeout(text ? noteApi.searchNotes(text) : noteApi.getNotes()),
        withRequestTimeout(noteApi.getHotNotes(5)),
      ])
      setNotes(Array.isArray(list) ? list : [])
      setHotNotes(Array.isArray(hot) ? hot : [])
    } catch (error) {
      setNotes([])
      setHotNotes([])
      setSearchNotice({
        tone: text ? 'warning' : 'error',
        message: isRequestTimeoutError(error) ? '游记数据加载超时，请稍后重试。' : text ? '游记搜索失败，请更换关键词后重试。' : '游记列表加载失败，请稍后重试。',
      })
    } finally {
      setLoading(false)
      setSearching(false)
      hasFetchedRef.current = true
    }
  }, [])

  useDebouncedKeywordEffect({ keyword: normalizedKeyword, delay: DEBOUNCE_DELAY, onKeywordChange: loadNotes })

  const commentFeedItems = useMemo(() => comments.map((item, index) => ({
    id: String(item.id || `comment-${index}`),
    author: String(item.username || item.userName || item.nickname || '匿名用户'),
    time: formatTime(item.createTime || item.createdAt),
    content: String(item.content || item.comment || '暂无评论内容'),
  })), [comments])

  const noteMetricItems = useMemo(() => [
    { label: '作者', value: String(detail?.author || selectedNote?.author || TEXT.ANONYMOUS) },
    { label: '点赞', value: detail?.likes || selectedNote?.likes || 0 },
    { label: '发布时间', value: formatTime(detail?.createTime || selectedNote?.createTime) },
  ], [detail, selectedNote])

  async function openDetail(note: TravelNote) {
    setSelectedNote(note)
    setDetailLoading(true)
    setDetailNotice(null)
    setCommentNotice(null)
    try {
      await noteApi.incrementViews(note.id).catch(() => false)
      const [d, c] = await Promise.allSettled([withRequestTimeout(noteApi.getNoteById(note.id)), withRequestTimeout(noteApi.getComments(note.id))])
      setDetail(d.status === 'fulfilled' ? d.value as Record<string, any> : null)
      setComments(c.status === 'fulfilled' && Array.isArray(c.value) ? c.value : [])
    } catch {
      setDetail(null)
      setComments([])
      setDetailNotice({ tone: 'error', message: '游记详情加载失败，请稍后重试。' })
    } finally {
      setDetailLoading(false)
    }
  }

  async function retryDetail() {
    if (!selectedNote) return
    await openDetail(selectedNote)
  }

  async function submitComment() {
    if (!selectedNote || !commentInput.trim()) return
    setCommentSubmitting(true)
    setCommentNotice(null)
    try {
      await noteApi.addComment(selectedNote.id, commentInput.trim())
      setCommentInput('')
      const list = await noteApi.getComments(selectedNote.id)
      setComments(Array.isArray(list) ? list : [])
    } catch {
      setCommentNotice({ tone: 'error', message: '评论提交失败，请稍后再试。' })
    } finally {
      setCommentSubmitting(false)
    }
  }

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />
        <div className="relative">
          <span className="section-kicker">旅行笔记</span>
          <div className="mt-3 flex flex-wrap gap-2"><span className="chip">真实体验</span><span className="chip">行程记录</span><span className="chip">灵感沉淀</span></div>
          <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">把旅途见闻沉淀成可回看的游记</h1>
          <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">记录真实体验、整理避坑建议，也能从别人的游记里快速吸收灵感。</p>
          <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <div className="metric-card"><div className="text-xs uppercase tracking-[0.18em] text-slate-400">内容总量</div><div className="mt-3 text-2xl font-semibold text-slate-900">{notes.length + hotNotes.length}</div></div>
            <div className="metric-card"><div className="text-xs uppercase tracking-[0.18em] text-slate-400">当前状态</div><div className="mt-3 text-base font-semibold text-slate-900">{normalizedKeyword ? '关键词筛选' : '全部游记'}</div></div>
            <div className="metric-card"><div className="text-xs uppercase tracking-[0.18em] text-slate-400">热门内容</div><div className="mt-3 text-2xl font-semibold text-slate-900">{hotNotes.length}</div></div>
            <div className="metric-card"><div className="text-xs uppercase tracking-[0.18em] text-slate-400">搜索结果</div><div className="mt-3 text-2xl font-semibold text-slate-900">{notes.length}</div></div>
          </div>
        </div>
      </section>

      <section className="mt-8 scenic-shell-soft p-6">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <span className="section-kicker">发现灵感</span>
            <h2 className="mt-3 section-heading">游记发现</h2>
            <p className="section-subtitle mt-2">搜索标题、作者或关键词，快速找到更适合当前行程的真实经验。</p>
          </div>
          <div className="w-full max-w-xl scenic-shell-soft p-5 sm:p-6">
            <div className="mb-4 flex items-center justify-between"><div><div className="text-sm font-medium text-slate-500">搜索游记</div><div className="mt-1 text-xl font-semibold text-slate-900">先看经验，再决定路线怎么调</div></div><span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">实时检索</span></div>
            <input value={keyword} onChange={(e) => setKeyword(e.target.value)} type="text" placeholder="搜索标题、作者或关键词" className="search-input" />
            {searchNotice ? <div className="mt-4"><StatusNotice tone={searchNotice.tone} message={searchNotice.message} actionLabel="重试" onAction={() => loadNotes(normalizedKeyword)} /></div> : null}
          </div>
        </div>
      </section>

      <section className="mt-8 scenic-shell-soft p-6">
        <span className="section-kicker">热门故事</span>
        <h2 className="mt-3 section-heading">热门游记</h2>
        <p className="section-subtitle mt-2">优先浏览互动更多、关注度更高的游记内容。</p>
        {searching ? <div className="mt-4"><LoadingSpinner /></div> : null}
        {!loading && !hotNotes.length ? <SearchEmptyState message="暂未找到热门游记。" actionLabel="重新加载" onAction={() => loadNotes(normalizedKeyword)} /> : null}
        {!loading && hotNotes.length ? <div className="mt-5 grid gap-5 md:grid-cols-2 xl:grid-cols-3">{hotNotes.map((item) => <article key={item.id} className="scenic-shell-soft surface-card-hover p-5">{item.image ? <img src={item.image} alt={item.title} className="mb-4 h-40 w-full rounded-2xl object-cover" /> : null}<div className="mb-4 flex items-center justify-between gap-3"><span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-slate-500">热门</span><span className="text-xs text-slate-400">{item.author || TEXT.ANONYMOUS}</span></div><h3 className="text-lg font-semibold text-slate-900">{item.title}</h3><p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-500">{item.excerpt || item.content || TEXT.NO_CONTENT}</p><div className="mt-4 flex gap-4 text-xs text-slate-400"><span>点赞 {item.likes || 0}</span><span>评论 {item.comments || 0}</span></div><button type="button" onClick={() => openDetail(item)} className="btn-secondary mt-4 w-full">查看详情</button></article>)}</div> : null}
      </section>

      <section className="mt-8">
        <div className="mb-5 flex items-center justify-between gap-3"><div><h2 className="section-heading text-[1.75rem]">全部游记</h2><p className="section-subtitle mt-2">浏览全部内容，继续筛选更适合当前计划的真实经验。</p></div><span className="chip">共 {notes.length} 篇</span></div>
        {!loading && !notes.length ? <SearchEmptyState message="暂无游记内容。" actionLabel="重新加载" onAction={() => loadNotes(normalizedKeyword)} /> : null}
        {!loading && notes.length ? <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">{notes.map((item) => <article key={item.id} className="scenic-shell-soft surface-card-hover p-5"><div className="mb-4 flex items-center justify-between gap-3"><span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-slate-500">游记</span><span className="text-xs text-slate-400">{item.author || TEXT.ANONYMOUS}</span></div><h3 className="text-lg font-semibold text-slate-900">{item.title}</h3><p className="mt-3 line-clamp-4 text-sm leading-6 text-slate-500">{item.excerpt || item.content || TEXT.NO_CONTENT}</p><div className="mt-5 flex items-center gap-4 text-xs text-slate-400"><span>点赞 {item.likes || 0}</span><span>评论 {item.comments || 0}</span><span>可展开阅读</span></div><button type="button" onClick={() => openDetail(item)} className="btn-secondary mt-4 w-full">查看详情</button></article>)}</div> : null}
      </section>

      <DetailDrawer open={Boolean(selectedNote)} title={selectedNote?.title || '游记详情'} subtitle={selectedNote?.author || '匿名作者'} loading={detailLoading} onClose={() => { setSelectedNote(null); setDetail(null); setComments([]); setCommentInput(''); setDetailNotice(null); setCommentNotice(null) }}>
        <div className="space-y-6">
          {detailNotice ? <StatusNotice tone={detailNotice.tone} message={detailNotice.message} actionLabel="重试" onAction={retryDetail} /> : null}
          {detail?.image || selectedNote?.image ? <img src={String(detail?.image || selectedNote?.image)} alt={selectedNote?.title || '游记封面'} className="h-56 w-full rounded-3xl object-cover" /> : null}
          <DetailTextCard>{String(detail?.content || detail?.excerpt || selectedNote?.content || selectedNote?.excerpt || TEXT.NO_CONTENT)}</DetailTextCard>
          <DetailMetricsGrid items={noteMetricItems} columnsClassName="sm:grid-cols-3" />
          <CommentFeed title="评论区" items={commentFeedItems} emptyText="还没有评论，欢迎留下第一条看法。" />
          {commentNotice ? <StatusNotice tone={commentNotice.tone} message={commentNotice.message} actionLabel={commentNotice.tone === 'error' ? '重试' : undefined} onAction={commentNotice.tone === 'error' ? submitComment : undefined} /> : null}
          {hasToken ? <CommentComposer title="发表评论" value={commentInput} onChange={setCommentInput} onSubmit={submitComment} submitting={commentSubmitting} disabled={commentSubmitting} placeholder="写下你的看法、补充经验或建议..." submitText="提交评论" submittingText="提交中..." /> : null}
        </div>
      </DetailDrawer>
    </div>
  )
}
