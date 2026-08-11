import { useMemo, useState, type ReactNode } from 'react'
import { realtimeApi } from '../api/realtime.api'
import { RealtimeCard } from '../components/RealtimeCard'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { SearchEmptyState } from '../components/common/SearchFeedback'
import { type StatusNoticeTone, StatusNotice } from '../components/common/StatusNotice'
import { CROWD_LEVEL_MEDIUM } from '../constants'

const DEFAULT_SYNC_MINUTES = 30

type QuerySection = 'single' | 'batch' | 'crowded' | 'historical' | 'sevenDays' | 'needSync' | 'traffic'

interface InlineNotice {
  tone: StatusNoticeTone
  message: string
}

function getTrafficStatusClass(value: Record<string, any> | null) {
  const status = String(value?.status || value?.level || '').toLowerCase()

  if (status.includes('拥堵') || status.includes('严重') || status.includes('severe') || status.includes('busy')) {
    return 'text-red-600'
  }

  if (status.includes('缓慢') || status.includes('slow') || status.includes('medium')) {
    return 'text-sky-600'
  }

  return 'text-emerald-600'
}

function getWarnLevelLabel(severity?: string) {
  if (severity === 'high') return '高风险'
  if (severity === 'medium') return '中风险'
  return '低风险'
}

function getWarnLevelClass(severity?: string) {
  if (severity === 'high') return 'bg-red-200 text-red-700'
  if (severity === 'medium') return 'bg-sky-100 text-sky-700'
  return 'bg-blue-200 text-blue-700'
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

function StatTile({ label, value }: { label: string; value: number }) {
  return (
    <div className="metric-card rounded-2xl px-4 py-4">
      <div className="text-xs text-slate-500">{label}</div>
      <div className="mt-2 text-2xl font-semibold text-slate-900">{value}</div>
    </div>
  )
}

export function RealtimeStatusPage() {
  const [singleAttractionId, setSingleAttractionId] = useState('')
  const [singleResult, setSingleResult] = useState<Record<string, any> | null>(null)
  const [batchIds, setBatchIds] = useState('')
  const [batchResults, setBatchResults] = useState<Record<string, any>[]>([])
  const [minCrowdLevel, setMinCrowdLevel] = useState(String(CROWD_LEVEL_MEDIUM))
  const [crowdedList, setCrowdedList] = useState<Record<string, any>[]>([])
  const [historicalAvgId, setHistoricalAvgId] = useState('')
  const [historicalAvgResult, setHistoricalAvgResult] = useState<number | null>(null)
  const [sevenDaysAvgId, setSevenDaysAvgId] = useState('')
  const [sevenDaysAvgResult, setSevenDaysAvgResult] = useState<number | null>(null)
  const [warns, setWarns] = useState<Record<string, any>[]>([])
  const [warnsLoading, setWarnsLoading] = useState(false)
  const [needSyncMinutes, setNeedSyncMinutes] = useState(String(DEFAULT_SYNC_MINUTES))
  const [needSyncList, setNeedSyncList] = useState<Record<string, any>[]>([])
  const [trafficAttractionId, setTrafficAttractionId] = useState('')
  const [trafficResult, setTrafficResult] = useState<Record<string, any> | null>(null)
  const [batchUpdateLoading, setBatchUpdateLoading] = useState(false)
  const [batchUpdateResult, setBatchUpdateResult] = useState('')
  const [loadingSection, setLoadingSection] = useState<QuerySection | null>(null)
  const [singleNotice, setSingleNotice] = useState<InlineNotice | null>(null)
  const [batchNotice, setBatchNotice] = useState<InlineNotice | null>(null)
  const [crowdedNotice, setCrowdedNotice] = useState<InlineNotice | null>(null)
  const [historicalNotice, setHistoricalNotice] = useState<InlineNotice | null>(null)
  const [sevenDaysNotice, setSevenDaysNotice] = useState<InlineNotice | null>(null)
  const [warnsNotice, setWarnsNotice] = useState<InlineNotice | null>(null)
  const [needSyncNotice, setNeedSyncNotice] = useState<InlineNotice | null>(null)
  const [trafficNotice, setTrafficNotice] = useState<InlineNotice | null>(null)
  const [hasFetchedSingle, setHasFetchedSingle] = useState(false)
  const [hasFetchedBatch, setHasFetchedBatch] = useState(false)
  const [hasFetchedCrowded, setHasFetchedCrowded] = useState(false)
  const [hasFetchedHistorical, setHasFetchedHistorical] = useState(false)
  const [hasFetchedSevenDays, setHasFetchedSevenDays] = useState(false)
  const [hasFetchedWarns, setHasFetchedWarns] = useState(false)
  const [hasFetchedNeedSync, setHasFetchedNeedSync] = useState(false)
  const [hasFetchedTraffic, setHasFetchedTraffic] = useState(false)

  const trafficStatusColor = useMemo(() => getTrafficStatusClass(trafficResult), [trafficResult])
  const batchUpdateTone = useMemo<StatusNoticeTone | null>(() => {
    if (!batchUpdateResult) return null
    return batchUpdateResult.includes('\u6210\u529f') ? 'success' : 'error'
  }, [batchUpdateResult])

  function isLoading(section: QuerySection) {
    return loadingSection === section
  }

  async function fetchSingleStatus() {
    const id = singleAttractionId.trim()
    if (!id) {
      setHasFetchedSingle(false)
      setSingleResult(null)
      setSingleNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u666f\u70b9\u7f16\u53f7\u3002' })
      return
    }

    setHasFetchedSingle(true)
    setSingleNotice(null)
    setLoadingSection('single')
    try {
      const result = await realtimeApi.getAttractionRealtimeStatus(Number(id))
      setSingleResult(result || null)
    } catch {
      setSingleResult(null)
      setSingleNotice({ tone: 'error', message: '\u5355\u666f\u70b9\u5b9e\u65f6\u72b6\u6001\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  async function fetchBatchStatus() {
    const ids = batchIds
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
      .map(Number)

    if (!ids.length) {
      setHasFetchedBatch(false)
      setBatchResults([])
      setBatchNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u9700\u8981\u67e5\u8be2\u7684\u666f\u70b9\u7f16\u53f7\u3002' })
      return
    }

    setHasFetchedBatch(true)
    setBatchNotice(null)
    setLoadingSection('batch')
    try {
      const result = await realtimeApi.getBatchRealtimeStatus(ids)
      setBatchResults(Array.isArray(result) ? result : [])
    } catch {
      setBatchResults([])
      setBatchNotice({ tone: 'error', message: '\u6279\u91cf\u72b6\u6001\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  async function fetchCrowdedList() {
    const level = Number(minCrowdLevel) || CROWD_LEVEL_MEDIUM

    setHasFetchedCrowded(true)
    setCrowdedNotice(null)
    setLoadingSection('crowded')
    try {
      const result = await realtimeApi.getCrowdedAttractions(level)
      setCrowdedList(Array.isArray(result) ? result : [])
    } catch {
      setCrowdedList([])
      setCrowdedNotice({ tone: 'error', message: '\u62e5\u6324\u666f\u70b9\u5217\u8868\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  async function fetchHistoricalAvg() {
    const id = historicalAvgId.trim()
    if (!id) {
      setHasFetchedHistorical(false)
      setHistoricalAvgResult(null)
      setHistoricalNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u666f\u70b9\u7f16\u53f7\u3002' })
      return
    }

    setHasFetchedHistorical(true)
    setHistoricalNotice(null)
    setLoadingSection('historical')
    try {
      const result = await realtimeApi.getHistoricalAvgCrowdCount(Number(id))
      const normalized = Number(result)
      setHistoricalAvgResult(Number.isFinite(normalized) ? normalized : null)
    } catch {
      setHistoricalAvgResult(null)
      setHistoricalNotice({ tone: 'error', message: '\u5386\u53f2\u5e73\u5747\u5ba2\u6d41\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  async function fetchSevenDaysAvg() {
    const id = sevenDaysAvgId.trim()
    if (!id) {
      setHasFetchedSevenDays(false)
      setSevenDaysAvgResult(null)
      setSevenDaysNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u666f\u70b9\u7f16\u53f7\u3002' })
      return
    }

    setHasFetchedSevenDays(true)
    setSevenDaysNotice(null)
    setLoadingSection('sevenDays')
    try {
      const result = await realtimeApi.get7DaysAvgCrowdCount(Number(id))
      const normalized = Number(result)
      setSevenDaysAvgResult(Number.isFinite(normalized) ? normalized : null)
    } catch {
      setSevenDaysAvgResult(null)
      setSevenDaysNotice({ tone: 'error', message: '\u8fd1 7 \u5929\u5e73\u5747\u5ba2\u6d41\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  async function fetchWarns() {
    setHasFetchedWarns(true)
    setWarnsNotice(null)
    setWarnsLoading(true)
    try {
      const result = await realtimeApi.getActiveWarns()
      setWarns(Array.isArray(result) ? result : [])
    } catch {
      setWarns([])
      setWarnsNotice({ tone: 'error', message: '\u6d3b\u52a8\u9884\u8b66\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setWarnsLoading(false)
    }
  }

  async function fetchNeedSync() {
    const minutes = Number(needSyncMinutes) || DEFAULT_SYNC_MINUTES

    setHasFetchedNeedSync(true)
    setNeedSyncNotice(null)
    setLoadingSection('needSync')
    try {
      const result = await realtimeApi.getNeedSyncStatus(minutes)
      setNeedSyncList(Array.isArray(result) ? result : [])
    } catch {
      setNeedSyncList([])
      setNeedSyncNotice({ tone: 'error', message: '\u5f85\u540c\u6b65\u72b6\u6001\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  async function fetchTraffic() {
    const id = trafficAttractionId.trim()
    if (!id) {
      setHasFetchedTraffic(false)
      setTrafficResult(null)
      setTrafficNotice({ tone: 'warning', message: '\u8bf7\u5148\u8f93\u5165\u666f\u70b9\u7f16\u53f7\u3002' })
      return
    }

    setHasFetchedTraffic(true)
    setTrafficNotice(null)
    setTrafficResult(null)
    setLoadingSection('traffic')
    try {
      const result = await realtimeApi.getTrafficInfo(Number(id))
      setTrafficResult(result || null)
    } catch {
      setTrafficResult(null)
      setTrafficNotice({ tone: 'error', message: '\u4ea4\u901a\u4fe1\u606f\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002' })
    } finally {
      setLoadingSection(null)
    }
  }

  async function batchUpdateStatus() {
    setBatchUpdateResult('')
    setBatchUpdateLoading(true)
    try {
      await realtimeApi.triggerBatchUpdate()
      setBatchUpdateResult('批量状态刷新成功')
    } catch {
      setBatchUpdateResult('批量状态刷新失败')
    } finally {
      setBatchUpdateLoading(false)
    }
  }

  const overviewCards = [
    { label: '批量状态', value: batchResults.length },
    { label: '拥挤景点', value: crowdedList.length },
    { label: '活动预警', value: warns.length },
    { label: '待同步项', value: needSyncList.length },
  ]

  return (
    <div className="app-container pb-16 pt-4 md:pt-6">
      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">
        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />
        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />

        <div className="relative">
        <div className="grid gap-8 xl:grid-cols-[1.08fr_0.92fr] xl:items-center">
          <div>
            <span className="section-kicker">实时状态</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">实时动态</span>
              <span className="chip">客流与预警</span>
              <span className="chip">交通状态</span>
            </div>
            <h1 className="text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">把景点状态、拥挤度和交通信息放在同一块看</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              这一页现在更适合做出发前和途中判断：先查单个景点，再看批量状态、拥挤列表、活动预警和交通信息，减少来回切页的成本。
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {overviewCards.map((item) => (
                <StatTile key={item.label} label={item.label} value={item.value} />
              ))}
            </div>
          </div>

          <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
            <div className="flex items-center justify-between gap-3">
              <div>
                <div className="text-sm font-medium text-slate-500">快捷操作</div>
                <div className="mt-1 text-xl font-semibold text-slate-900">先刷新状态，再决定是否调整路线</div>
              </div>
              <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">
                {'\u5b9e\u65f6\u770b\u677f'}
              </span>
            </div>
            <div className="mt-4 grid gap-3 text-sm text-slate-600">
              <div className="travel-step-card">单景点查询适合出发前确认排队和天气。</div>
              <div className="travel-step-card">拥挤列表和预警适合中途决定是否临时换点。</div>
              <div className="travel-step-card">交通状态可以辅助判断是否继续按原路线前进。</div>
            </div>
            <div className="mt-5 flex flex-wrap gap-3">
              <button type="button" onClick={batchUpdateStatus} className="btn-primary">
                {batchUpdateLoading ? '刷新中...' : '批量刷新状态'}
              </button>
            {batchUpdateTone ? <StatusNotice tone={batchUpdateTone} message={batchUpdateResult} className="mt-4" /> : null}
            </div>
          </div>
        </div>
        </div>
      </section>

      <section className="mt-8 grid gap-6 xl:grid-cols-[1fr_1fr]">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="单景点状态" description="输入景点编号，查看当前客流、等待时间、天气与开闭园状态。" />
          <div className="flex gap-3">
            <input
              value={singleAttractionId}
              onChange={(event) => setSingleAttractionId(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u666f\u70b9\u7f16\u53f7'}
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchSingleStatus}>查询</button>
          </div>
          {singleNotice ? (
            <StatusNotice
              tone={singleNotice.tone}
              message={singleNotice.message}
              actionLabel={singleNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={singleNotice.tone === 'error' ? fetchSingleStatus : undefined}
              className="mt-4"
            />
          ) : null}
          {isLoading('single') ? <div className="mt-4"><LoadingSpinner /></div> : null}
          {!isLoading('single') && singleResult ? <div className="mt-4"><RealtimeCard data={singleResult} /></div> : null}
          {!isLoading('single') && hasFetchedSingle && !singleResult && !singleNotice ? (
            <SearchEmptyState className="mt-4" message={'\u6682\u672a\u67e5\u8be2\u5230\u8be5\u666f\u70b9\u7684\u5b9e\u65f6\u6570\u636e\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchSingleStatus} />
          ) : null}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="批量状态" description="多个景点一起查询，更适合对比当天行程中几个候选点。" />
          <div className="flex gap-3">
            <input
              value={batchIds}
              onChange={(event) => setBatchIds(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u591a\u4e2a\u666f\u70b9\u7f16\u53f7\uff0c\u7528\u9017\u53f7\u5206\u9694'}
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchBatchStatus}>查询</button>
          </div>
          {batchNotice ? (
            <StatusNotice
              tone={batchNotice.tone}
              message={batchNotice.message}
              actionLabel={batchNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={batchNotice.tone === 'error' ? fetchBatchStatus : undefined}
              className="mt-4"
            />
          ) : null}
          {isLoading('batch') ? <div className="mt-4"><LoadingSpinner /></div> : null}
          {!isLoading('batch') && batchResults.length ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {batchResults.map((item, index) => (
                <RealtimeCard key={item.id || `${item.attractionId}-${index}`} data={item} />
              ))}
            </div>
          ) : null}
          {!isLoading('batch') && hasFetchedBatch && !batchResults.length && !batchNotice ? (
            <SearchEmptyState className="mt-4" message={'\u5f53\u524d\u6ca1\u6709\u53ef\u5c55\u793a\u7684\u6279\u91cf\u72b6\u6001\u7ed3\u679c\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchBatchStatus} />
          ) : null}
        </div>
      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-[1fr_1fr]">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="拥挤景点" description="按最低拥挤等级筛出当前更需要回避的景点。" />
          <div className="flex gap-3">
            <input
              value={minCrowdLevel}
              onChange={(event) => setMinCrowdLevel(event.target.value)}
              type="text"
              placeholder="输入最小拥挤等级"
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchCrowdedList}>查询</button>
          </div>
          {crowdedNotice ? (
            <StatusNotice
              tone={crowdedNotice.tone}
              message={crowdedNotice.message}
              actionLabel={crowdedNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={crowdedNotice.tone === 'error' ? fetchCrowdedList : undefined}
              className="mt-4"
            />
          ) : null}
          {isLoading('crowded') ? <div className="mt-4"><LoadingSpinner /></div> : null}
          {!isLoading('crowded') && crowdedList.length ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {crowdedList.map((item, index) => (
                <RealtimeCard key={item.id || `${item.attractionId}-${index}`} data={item} />
              ))}
            </div>
          ) : null}
          {!isLoading('crowded') && hasFetchedCrowded && !crowdedList.length && !crowdedNotice ? (
            <SearchEmptyState className="mt-4" message={'\u5f53\u524d\u6ca1\u6709\u8fbe\u5230\u8be5\u62e5\u6324\u7b49\u7ea7\u7684\u666f\u70b9\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchCrowdedList} />
          ) : null}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="历史客流参考" description="把历史平均值和近 7 天均值放在一起，辅助判断今天是否异常拥挤。" />
          <div className="grid gap-4 md:grid-cols-2">
            <div className="metric-card">
              <div className="text-xs text-slate-500">历史平均客流</div>
              <div className="mt-3 flex gap-3">
                <input
                  value={historicalAvgId}
                  onChange={(event) => setHistoricalAvgId(event.target.value)}
                  type="text"
                  placeholder={'\u8f93\u5165\u666f\u70b9\u7f16\u53f7'}
                  className="search-input flex-1"
                />
                <button className="btn-secondary" onClick={fetchHistoricalAvg}>查询</button>
              </div>
              {historicalNotice ? (
                <StatusNotice
                  tone={historicalNotice.tone}
                  message={historicalNotice.message}
                  actionLabel={historicalNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
                  onAction={historicalNotice.tone === 'error' ? fetchHistoricalAvg : undefined}
                  className="mt-4"
                />
              ) : null}
              {isLoading('historical') ? <div className="mt-4"><LoadingSpinner /></div> : null}
{!isLoading('historical') && historicalAvgResult !== null ? <div className="mt-4 text-sm text-slate-600">{'\u7ed3\u679c\uff1a'}{historicalAvgResult}</div> : null}
              {!isLoading('historical') && hasFetchedHistorical && historicalAvgResult === null && !historicalNotice ? (
                <SearchEmptyState className="mt-4 p-6" message={'\u6682\u672a\u83b7\u53d6\u5230\u5386\u53f2\u5e73\u5747\u5ba2\u6d41\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchHistoricalAvg} />
              ) : null}
            </div>
            <div className="metric-card">
              <div className="text-xs text-slate-500">近 7 天平均客流</div>
              <div className="mt-3 flex gap-3">
                <input
                  value={sevenDaysAvgId}
                  onChange={(event) => setSevenDaysAvgId(event.target.value)}
                  type="text"
                  placeholder={'\u8f93\u5165\u666f\u70b9\u7f16\u53f7'}
                  className="search-input flex-1"
                />
                <button className="btn-secondary" onClick={fetchSevenDaysAvg}>查询</button>
              </div>
              {sevenDaysNotice ? (
                <StatusNotice
                  tone={sevenDaysNotice.tone}
                  message={sevenDaysNotice.message}
                  actionLabel={sevenDaysNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
                  onAction={sevenDaysNotice.tone === 'error' ? fetchSevenDaysAvg : undefined}
                  className="mt-4"
                />
              ) : null}
              {isLoading('sevenDays') ? <div className="mt-4"><LoadingSpinner /></div> : null}
{!isLoading('sevenDays') && sevenDaysAvgResult !== null ? <div className="mt-4 text-sm text-slate-600">{'\u7ed3\u679c\uff1a'}{sevenDaysAvgResult}</div> : null}
              {!isLoading('sevenDays') && hasFetchedSevenDays && sevenDaysAvgResult === null && !sevenDaysNotice ? (
                <SearchEmptyState className="mt-4 p-6" message={'\u6682\u672a\u83b7\u53d6\u5230\u8fd1 7 \u5929\u5e73\u5747\u5ba2\u6d41\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchSevenDaysAvg} />
              ) : null}
            </div>
          </div>
        </div>
      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-[1fr_1fr]">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="活动预警" description="查看当前激活的状态预警与风险提示。" action={<button className="btn-secondary" onClick={fetchWarns}>刷新</button>} />
          {warnsNotice ? (
            <StatusNotice
              tone={warnsNotice.tone}
              message={warnsNotice.message}
              actionLabel={warnsNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={warnsNotice.tone === 'error' ? fetchWarns : undefined}
              className="mb-4"
            />
          ) : null}
          {warnsLoading ? <LoadingSpinner /> : null}
          {!warnsLoading && warns.length ? (
            <div className="space-y-4">
              {warns.map((warn, index) => (
                <article key={warn.id || index} className="rounded-[1.5rem] border border-slate-200 bg-slate-50 px-5 py-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="text-base font-semibold text-slate-900">{warn.title || warn.name || `\u9884\u8b66\u7f16\u53f7 ${index + 1}`}</h3>
                      <p className="mt-2 text-sm leading-6 text-slate-500">{warn.description || warn.message || '\u6682\u65e0\u66f4\u591a\u63cf\u8ff0'}</p>
                    </div>
                    <span className={`rounded-full px-3 py-1 text-xs font-medium ${getWarnLevelClass(warn.severity)}`}>
                      {getWarnLevelLabel(warn.severity)}
                    </span>
                  </div>
                </article>
              ))}
            </div>
          ) : null}
          {!warnsLoading && hasFetchedWarns && !warns.length && !warnsNotice ? (
            <SearchEmptyState className="mt-4" message={'\u5f53\u524d\u6682\u65e0\u6d3b\u52a8\u9884\u8b66\u3002'} actionLabel={'\u91cd\u65b0\u5237\u65b0'} onAction={fetchWarns} />
          ) : null}
        </div>

        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="待同步状态" description="查询最近一段时间内需要重新同步的实时状态记录。" />
          <div className="flex gap-3">
            <input
              value={needSyncMinutes}
              onChange={(event) => setNeedSyncMinutes(event.target.value)}
              type="text"
              placeholder="输入分钟数，例如 30"
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchNeedSync}>查询</button>
          </div>
          {needSyncNotice ? (
            <StatusNotice
              tone={needSyncNotice.tone}
              message={needSyncNotice.message}
              actionLabel={needSyncNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={needSyncNotice.tone === 'error' ? fetchNeedSync : undefined}
              className="mt-4"
            />
          ) : null}
          {isLoading('needSync') ? <div className="mt-4"><LoadingSpinner /></div> : null}
          {!isLoading('needSync') && needSyncList.length ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {needSyncList.map((item, index) => (
                <RealtimeCard key={item.id || `${item.attractionId}-${index}`} data={item} />
              ))}
            </div>
          ) : null}
          {!isLoading('needSync') && hasFetchedNeedSync && !needSyncList.length && !needSyncNotice ? (
            <SearchEmptyState className="mt-4" message={'\u5f53\u524d\u6682\u65e0\u5f85\u540c\u6b65\u72b6\u6001\u8bb0\u5f55\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchNeedSync} />
          ) : null}
        </div>
      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-[1fr_1fr]">
        <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
          <SectionHeader title="交通信息查询" description="快速查看指定景点周边交通状态、速度和拥堵指数。" />
          <div className="flex gap-3">
            <input
              value={trafficAttractionId}
              onChange={(event) => setTrafficAttractionId(event.target.value)}
              type="text"
              placeholder={'\u8f93\u5165\u666f\u70b9\u7f16\u53f7'}
              className="search-input flex-1"
            />
            <button className="btn-primary" onClick={fetchTraffic}>查询</button>
          </div>
          {trafficNotice ? (
            <StatusNotice
              tone={trafficNotice.tone}
              message={trafficNotice.message}
              actionLabel={trafficNotice.tone === 'error' ? '\u91cd\u8bd5' : undefined}
              onAction={trafficNotice.tone === 'error' ? fetchTraffic : undefined}
              className="mt-4"
            />
          ) : null}
          {isLoading('traffic') ? <div className="mt-4"><LoadingSpinner /></div> : null}
          {!isLoading('traffic') && trafficResult ? (
            <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="space-y-2 text-sm">
                <p className="text-slate-700">
                  {'\u4ea4\u901a\u72b6\u6001\uff1a'}<span className={trafficStatusColor}>{String(trafficResult.status || trafficResult.level || '\u672a\u77e5')}</span>
                </p>
                {trafficResult.speed ? <p className="text-slate-600">{'\u5e73\u5747\u901f\u5ea6\uff1a'}{trafficResult.speed} km/h</p> : null}
                {trafficResult.congestion ? <p className="text-slate-600">{'\u62e5\u5835\u6307\u6570\uff1a'}{trafficResult.congestion}</p> : null}
                {trafficResult.description ? <p className="text-xs text-slate-500">{trafficResult.description}</p> : null}
              </div>
            </div>
          ) : null}
          {!isLoading('traffic') && hasFetchedTraffic && !trafficResult && !trafficNotice ? (
            <SearchEmptyState className="mt-4" message={'\u6682\u672a\u67e5\u8be2\u5230\u8be5\u666f\u70b9\u7684\u4ea4\u901a\u4fe1\u606f\u3002'} actionLabel={'\u91cd\u65b0\u67e5\u8be2'} onAction={fetchTraffic} />
          ) : null}
        </div>
      </section>
    </div>
  )
}


