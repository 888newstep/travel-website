import { useState, type ReactNode } from 'react'
import { shareApi, type RouteShare, type ShareStatistics } from '../api/share.api'
import { SearchEmptyState } from '../components/common/SearchFeedback'
import { type StatusNoticeTone, StatusNotice } from '../components/common/StatusNotice'
import { DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_PAGE_SIZE } from '../constants'

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

function getShareItemTypeLabel(itemType?: string) {
  return itemType === 'note' ? '\u7b14\u8bb0' : '\u8def\u7ebf'
}

interface InlineNotice {
  tone: StatusNoticeTone
  message: string
}

export function RouteSharePage() {
  const [genForm, setGenForm] = useState({ itemId: '', itemType: 'route' })
  const [genResult, setGenResult] = useState<RouteShare | null>(null)
  const [validateCode, setValidateCode] = useState('')
  const [validateResult, setValidateResult] = useState<boolean | null>(null)
  const [listUserId, setListUserId] = useState('')
  const [shareList, setShareList] = useState<RouteShare[]>([])
  const [popularList, setPopularList] = useState<RouteShare[]>([])
  const [statsId, setStatsId] = useState('')
  const [statsResult, setStatsResult] = useState<ShareStatistics | null>(null)
  const [batchIds, setBatchIds] = useState('')
  const [pageNotice, setPageNotice] = useState<InlineNotice | null>(null)
  const [listNotice, setListNotice] = useState<InlineNotice | null>(null)
  const [popularNotice, setPopularNotice] = useState<InlineNotice | null>(null)
  const [statsNotice, setStatsNotice] = useState<InlineNotice | null>(null)
  const [hasLoadedShares, setHasLoadedShares] = useState(false)
  const [hasLoadedPopular, setHasLoadedPopular] = useState(false)
  const [hasLoadedStats, setHasLoadedStats] = useState(false)

  async function generateShare() {
    if (!genForm.itemId.trim()) {
      setPageNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u5185\u5bb9\u7f16\u53f7\u3002' })
      setGenResult(null)
      return
    }

    setPageNotice(null)
    try {
      const response = await shareApi.generateShareCode(Number(genForm.itemId), genForm.itemType || 'route')
      setGenResult(response || null)
      setPageNotice({ tone: 'success', message: '\u5206\u4eab\u7801\u5df2\u751f\u6210\uff0c\u53ef\u4ee5\u7ee7\u7eed\u590d\u5236\u6216\u6821\u9a8c\u3002' })
    } catch {
      setGenResult(null)
      setPageNotice({ tone: 'error', message: '\u5206\u4eab\u7801\u751f\u6210\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    }
  }

  async function validateShare() {
    if (!validateCode.trim()) {
      setPageNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u5206\u4eab\u7801\u3002' })
      setValidateResult(null)
      return
    }

    setPageNotice(null)
    try {
      const response = await shareApi.validateShareCode(validateCode.trim())
      setValidateResult(Boolean(response))
    } catch {
      setValidateResult(null)
      setPageNotice({ tone: 'error', message: '\u5206\u4eab\u7801\u6821\u9a8c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\u3002' })
    }
  }

  async function fetchUserShares() {
    if (!listUserId.trim()) {
      setHasLoadedShares(false)
      setListNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u7528\u6237\u7f16\u53f7\u3002' })
      setShareList([])
      return
    }

    setHasLoadedShares(true)
    setListNotice(null)
    try {
      const response = await shareApi.getUserShares(Number(listUserId) || 0, DEFAULT_PAGE, DEFAULT_PAGE_SIZE)
      setShareList(Array.isArray(response) ? response : [])
    } catch {
      setShareList([])
      setListNotice({ tone: 'error', message: '\u5206\u4eab\u8bb0\u5f55\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    }
  }

  async function fetchPopular() {
    setHasLoadedPopular(true)
    setPopularNotice(null)
    try {
      const response = await shareApi.getPopularShares(DEFAULT_LIMIT)
      setPopularList(Array.isArray(response) ? response : [])
    } catch {
      setPopularList([])
      setPopularNotice({ tone: 'error', message: '\u70ed\u95e8\u5206\u4eab\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    }
  }

  async function fetchStats() {
    if (!statsId.trim()) {
      setHasLoadedStats(false)
      setStatsNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u5185\u5bb9\u7f16\u53f7\u3002' })
      setStatsResult(null)
      return
    }

    setHasLoadedStats(true)
    setStatsNotice(null)
    try {
      const response = await shareApi.getShareStatistics(Number(statsId) || 0)
      setStatsResult(response && Object.keys(response).length ? response : null)
    } catch {
      setStatsResult(null)
      setStatsNotice({ tone: 'error', message: '\u5206\u4eab\u7edf\u8ba1\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    }
  }

  async function cancelShare(id?: number) {
    if (!id) return

    setPageNotice(null)
    try {
      await shareApi.cancelShare(id)
      setShareList((current) => current.filter((item) => item.id !== id))
      setPageNotice({ tone: 'success', message: '\u5206\u4eab\u5df2\u53d6\u6d88\u3002' })
    } catch {
      setPageNotice({ tone: 'error', message: '\u53d6\u6d88\u5206\u4eab\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\u3002' })
    }
  }

  async function batchCancel() {
    const ids = batchIds
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
      .map(Number)

    if (!ids.length) {
      setPageNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u9700\u8981\u53d6\u6d88\u7684\u5206\u4eab\u7f16\u53f7\u3002' })
      return
    }

    setPageNotice(null)
    try {
      await shareApi.batchCancelShares(ids)
      setShareList((current) => current.filter((item) => !ids.includes(Number(item.id))))
      setPageNotice({ tone: 'success', message: '\u6279\u91cf\u53d6\u6d88\u5df2\u63d0\u4ea4\uff0c\u53ef\u7ee7\u7eed\u5237\u65b0\u5206\u4eab\u5217\u8868\u3002' })
    } catch {
      setPageNotice({ tone: 'error', message: '\u6279\u91cf\u53d6\u6d88\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    }
  }

  const pendingBatchCount = batchIds
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean).length

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

        <div className="relative grid gap-6 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
          <div>
            <span className="section-kicker">路线分享</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">{'\u8def\u7ebf\u5206\u4eab'}</span>
              <span className="chip">{'\u9080\u8bf7\u7801'}</span>
              <span className="chip">{'\u8bbf\u95ee\u6570\u636e'}</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">{'\u5206\u4eab\u4f60\u7684\u884c\u7a0b\u4e0e\u8def\u7ebf'}</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              {'\u751f\u6210\u5206\u4eab\u7801\uff0c\u6821\u9a8c\u94fe\u63a5\u6709\u6548\u6027\u3001\u67e5\u770b\u8bbf\u95ee\u7edf\u8ba1\u4e0e\u70ed\u95e8\u5206\u4eab\uff0c\u4fbf\u4e8e\u5728\u65c5\u9014\u4e2d\u5feb\u901f\u5206\u53d1\u8def\u7ebf\u3002'}
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5df2\u751f\u6210'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{shareList.length}</div>
              </div>
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u70ed\u95e8\u6570'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{popularList.length}</div>
              </div>
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5f85\u5904\u7406'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{pendingBatchCount}</div>
              </div>
            </div>
          </div>

          <div className="scenic-shell-soft edge-glow animate-fade-in p-5 sm:p-6">
            <SectionHeader title={'\u5206\u4eab\u8bf4\u660e'} description={'\u4e09\u6b65\u5b8c\u6210\u8def\u7ebf\u5206\u4eab'} />
            <div className="grid gap-2 text-sm text-slate-500 sm:grid-cols-2">
              <div className="travel-step-card">{'\u5148\u751f\u6210\u5206\u4eab\u7801\uff0c\u518d\u590d\u5236\u7ed9\u540c\u4f34'}</div>
              <div className="travel-step-card">{'\u652f\u6301\u6821\u9a8c\u3001\u7edf\u8ba1\u548c\u6279\u91cf\u53d6\u6d88'}</div>
            </div>
          </div>
        </div>
      </section>

      {pageNotice ? <StatusNotice tone={pageNotice.tone} message={pageNotice.message} className="mt-6" /> : null}

      <div className="mt-8 grid gap-6 xl:grid-cols-2">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u751f\u6210\u5206\u4eab\u7801'} description={'\u8f93\u5165\u8def\u7ebf\u6216\u5185\u5bb9\u7f16\u53f7\u751f\u6210\u5206\u4eab\u7801'} />
          <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_auto]">
            <input
              value={genForm.itemId}
              onChange={(event) => setGenForm((current) => ({ ...current, itemId: event.target.value }))}
              type="text"
              placeholder={'\u8f93\u5165\u5185\u5bb9\u7f16\u53f7'}
              className="search-input"
            />
            <button onClick={generateShare} className="btn-primary">{'\u751f\u6210'}</button>
          </div>
          <select
            value={genForm.itemType}
            onChange={(event) => setGenForm((current) => ({ ...current, itemType: event.target.value }))}
            className="mt-3 search-input"
          >
            <option value="route">{'\u8def\u7ebf'}</option>
            <option value="note">{'\u7b14\u8bb0'}</option>
          </select>
          {genResult ? <pre className="mt-4 whitespace-pre-wrap rounded-2xl bg-slate-50 p-4 text-xs text-slate-600">{JSON.stringify(genResult, null, 2)}</pre> : null}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u6821\u9a8c\u5206\u4eab\u7801'} description={'\u8f93\u5165\u5206\u4eab\u7801\u6821\u9a8c\u662f\u5426\u6709\u6548'} />
          <div className="mt-4 flex gap-3">
            <input
              value={validateCode}
              onChange={(event) => setValidateCode(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u5206\u4eab\u7801'}
              className="search-input flex-1"
            />
            <button onClick={validateShare} className="btn-primary">{'\u6821\u9a8c'}</button>
          </div>
          {validateResult !== null ? (
            <div className="mt-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
              {validateResult ? '\u5206\u4eab\u7801\u6709\u6548' : '\u5206\u4eab\u7801\u65e0\u6548\u6216\u5df2\u5931\u6548'}
            </div>
          ) : null}
        </div>

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6 xl:col-span-2">
          <SectionHeader title={'\u6211\u7684\u5206\u4eab\u8bb0\u5f55'} description={'\u6309\u7528\u6237\u7f16\u53f7\u67e5\u8be2\u5206\u4eab\u4e0e\u70ed\u95e8\u8bb0\u5f55'} action={<span className="chip">{shareList.length} {'\u6761'}</span>} />
          <div className="mb-4 grid gap-3 sm:grid-cols-[1fr_auto_auto]">
            <input
              value={listUserId}
              onChange={(event) => setListUserId(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u7528\u6237\u7f16\u53f7'}
              className="search-input"
            />
            <button onClick={fetchUserShares} className="btn-secondary">{'\u67e5\u8be2\u6211\u7684\u5206\u4eab'}</button>
            <button onClick={fetchPopular} className="btn-secondary">{'\u67e5\u770b\u70ed\u95e8'}</button>
          </div>
          {listNotice ? (
            <StatusNotice
              tone={listNotice.tone}
              message={listNotice.message}
              actionLabel={listNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={listNotice.tone === 'error' ? fetchUserShares : undefined}
              className="mb-4"
            />
          ) : null}
          {shareList.length ? (
            <div className="space-y-4">
              {shareList.map((item) => (
                <article key={item.id} className="metric-card surface-card-hover">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <h3 className="text-base font-semibold text-slate-900">{item.shareCode || '\u672a\u547d\u540d\u7801'}</h3>
                      <p className="mt-2 text-sm leading-6 text-slate-500">{'\u7c7b\u578b '}{getShareItemTypeLabel(item.itemType)}{' \u00b7 \u5185\u5bb9\u7f16\u53f7 '}{item.itemId || item.routeId || '-'}</p>
                      <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-400">
                        <span className="chip">{'\u8bbf\u95ee'} {item.visitCount || 0}</span>
                        <span className="chip">{'\u5230\u671f'} {item.expireTime || '\u672a\u77e5'}</span>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => cancelShare(item.id)}
                      className="rounded-2xl border border-red-200 px-3 py-2 text-xs font-medium text-red-600 transition hover:bg-red-50"
                    >
                      {'\u53d6\u6d88\u5206\u4eab'}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          ) : hasLoadedShares ? (
            <SearchEmptyState
              message={'\u6682\u672a\u67e5\u5230\u5bf9\u5e94\u7684\u5206\u4eab\u8bb0\u5f55\uff0c\u53ef\u66f4\u6362\u7528\u6237\u7f16\u53f7\u540e\u518d\u8bd5\u3002'}
              actionLabel={'\u91cd\u65b0\u67e5\u8be2'}
              onAction={fetchUserShares}
            />
          ) : null}
        </section>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u5206\u4eab\u7edf\u8ba1'} description={'\u6309\u5185\u5bb9\u7f16\u53f7\u67e5\u770b\u5206\u4eab\u6982\u51b5'} />
          <div className="mt-4 flex gap-3">
            <input
              value={statsId}
              onChange={(event) => setStatsId(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u5185\u5bb9\u7f16\u53f7'}
              className="search-input flex-1"
            />
            <button onClick={fetchStats} className="btn-primary">{'\u67e5\u770b'}</button>
          </div>
          {statsNotice ? (
            <StatusNotice
              tone={statsNotice.tone}
              message={statsNotice.message}
              actionLabel={statsNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={statsNotice.tone === 'error' ? fetchStats : undefined}
              className="mt-4"
            />
          ) : null}
          {statsResult ? <pre className="mt-4 whitespace-pre-wrap rounded-2xl bg-slate-50 p-4 text-xs text-slate-600">{JSON.stringify(statsResult, null, 2)}</pre> : null}
          {hasLoadedStats && !statsResult && !statsNotice ? (
            <SearchEmptyState
              className="mt-4"
              message={'\u6682\u672a\u53d6\u5230\u8be5\u5185\u5bb9\u7684\u5206\u4eab\u7edf\u8ba1\u3002'}
              actionLabel={'\u91cd\u65b0\u67e5\u8be2'}
              onAction={fetchStats}
            />
          ) : null}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title={'\u6279\u91cf\u53d6\u6d88'} description={'\u8f93\u5165\u591a\u4e2a\u5206\u4eab\u7f16\u53f7\uff0c\u4ee5\u9017\u53f7\u5206\u9694'} />
          <div className="mt-4 space-y-3">
            <textarea
              value={batchIds}
              onChange={(event) => setBatchIds(event.target.value)}
              rows={5}
              placeholder={'\u8f93\u5165\u5206\u4eab\u7f16\u53f7\uff0c\u9017\u53f7\u5206\u9694'}
              className="search-input"
            />
            <button onClick={batchCancel} className="btn-primary">{'\u6279\u91cf\u53d6\u6d88'}</button>
          </div>
        </div>

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6 xl:col-span-2">
          <SectionHeader title={'\u70ed\u95e8\u5206\u4eab'} description={'\u67e5\u770b\u8bbf\u95ee\u91cf\u8f83\u9ad8\u7684\u5206\u4eab\u5185\u5bb9'} action={<span className="chip">{popularList.length} {'\u6761'}</span>} />
          {popularNotice ? (
            <StatusNotice
              tone={popularNotice.tone}
              message={popularNotice.message}
              actionLabel={popularNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={popularNotice.tone === 'error' ? fetchPopular : undefined}
              className="mb-4"
            />
          ) : null}
          {popularList.length ? (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {popularList.map((item) => (
                <article key={item.id} className="metric-card surface-card-hover">
                  <div className="text-sm font-semibold text-slate-900">{item.shareCode || '\u672a\u547d\u540d\u7801'}</div>
                  <div className="mt-2 text-sm text-slate-500">{'\u7c7b\u578b '}{getShareItemTypeLabel(item.itemType)}{' \u00b7 \u8bbf\u95ee '}{item.visitCount || 0}</div>
                </article>
              ))}
            </div>
          ) : hasLoadedPopular ? (
            <SearchEmptyState
              message={'\u6682\u65e0\u70ed\u95e8\u5206\u4eab\u6570\u636e\uff0c\u53ef\u7a0d\u540e\u91cd\u65b0\u52a0\u8f7d\u3002'}
              actionLabel={'\u91cd\u65b0\u52a0\u8f7d'}
              onAction={fetchPopular}
            />
          ) : null}
        </section>
      </div>
    </div>
  )
}
