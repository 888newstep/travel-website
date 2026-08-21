import { useEffect, useRef, useState } from 'react'



import { fileApi, type FileCategory, type ResourceFile } from '../api/file.api'



import { LoadingSpinner } from '../components/common/LoadingSpinner'



import { DEBOUNCE_DELAY } from '../constants'



function formatStatNumber(value: unknown, fallback: number) {



  const numberValue = Number(value)



  return Number.isFinite(numberValue) ? numberValue : fallback



}



export function FileManagementPage() {



  const [loading, setLoading] = useState(true)



  const [files, setFiles] = useState<ResourceFile[]>([])



  const [keyword, setKeyword] = useState('')



  const [stats, setStats] = useState<Record<string, any> | null>(null)



  const [showCategoryManager, setShowCategoryManager] = useState(false)



  const [newCategoryName, setNewCategoryName] = useState('')



  const [categories, setCategories] = useState<FileCategory[]>([])



  const [compareResult, setCompareResult] = useState<Record<string, any> | null>(null)



  const [versionModal, setVersionModal] = useState({



    open: false,



    loading: false,



    fileId: '',



    fileName: '',



    versions: [] as any[],



  })



  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)



  async function fetchFiles(searchText?: string) {



    setLoading(true)



    try {



      const normalized = (searchText ?? keyword).trim()



      const data = normalized ? await fileApi.searchFiles(normalized) : await fileApi.getFileList()



      const statistics = await fileApi.getFileStatistics()



      setFiles(Array.isArray(data) ? data : [])



      setStats(statistics || null)



    } catch {



      setFiles([])



      setStats(null)



    } finally {



      setLoading(false)



    }



  }



  async function fetchCategories() {



    try {



      const data = await fileApi.getCategories()



      setCategories(Array.isArray(data) ? data : [])



    } catch {



      setCategories([])



    }



  }



  useEffect(() => {



    fetchFiles('')



    fetchCategories()



    return () => {



      if (searchTimerRef.current) clearTimeout(searchTimerRef.current)



    }



    // eslint-disable-next-line react-hooks/exhaustive-deps



  }, [])



  function doSearch(nextKeyword: string) {



    setKeyword(nextKeyword)



    if (searchTimerRef.current) clearTimeout(searchTimerRef.current)



    searchTimerRef.current = setTimeout(() => {



      fetchFiles(nextKeyword)



    }, DEBOUNCE_DELAY)



  }



  async function uploadFile(event: React.ChangeEvent<HTMLInputElement>) {



    const file = event.target.files?.[0]



    if (!file) return



    const formData = new FormData()



    formData.append('file', file)



    try {



      await fileApi.uploadFile(formData)



      event.target.value = ''



      fetchFiles('')



    } catch {



      // ignore



    }



  }



  async function deleteFile(item: ResourceFile) {



    if (!item.id) return



    if (!window.confirm(`确认删除文件 "${item.fileName}" 吗？`)) return



    try {



      await fileApi.deleteFile(item.id)



      setFiles((current) => current.filter((entry) => entry.id !== item.id))



    } catch {



      // ignore



    }



  }



  async function createCategory() {



    if (!newCategoryName.trim()) return



    try {



      await fileApi.createCategory({ tagName: newCategoryName.trim() })



      setNewCategoryName('')



      fetchCategories()



    } catch {



      // ignore



    }



  }



  async function deleteCategory(id?: number) {



    if (!id) return



    try {



      await fileApi.deleteCategory(String(id))



      fetchCategories()



    } catch {



      // ignore



    }



  }



  async function showVersions(item: ResourceFile) {



    if (!item.id) return



    setVersionModal({ open: true, loading: true, fileId: String(item.id), fileName: item.fileName, versions: [] })



    setCompareResult(null)



    try {



      const data = await fileApi.getFileVersions(String(item.id))



      setVersionModal({



        open: true,



        loading: false,



        fileId: String(item.id),



        fileName: item.fileName,



        versions: Array.isArray(data) ? data : [],



      })



    } catch {



      setVersionModal({ open: true, loading: false, fileId: String(item.id), fileName: item.fileName, versions: [] })



    }



  }



  async function compareWithLatest(version: Record<string, any>) {



    setCompareResult(null)



    try {



      const result = await fileApi.getFileVersions(versionModal.fileId)



      const versions = Array.isArray(result) ? result : []



      if (versions.length >= 2) {



        const latestVersion = versions[0]



        const compare = await fileApi.compareVersions(String(version.id || versions[versions.length - 1].id), String(latestVersion.id))



        setCompareResult(compare || { version1: version.version || '未知版本', version2: '最新版本', diff: '暂无差异信息' })



      } else {



        setCompareResult({ message: '至少需要 1 个历史版本才能对比' })



      }



    } catch (error) {



      const message = error instanceof Error ? error.message : '操作失败'



      setCompareResult({ message })



    }



  }



  const totalFiles = formatStatNumber(stats?.totalFiles, files.length)



  const totalCategories = formatStatNumber(stats?.totalCategories, categories.length)



  return (



    <div className="app-container pb-16 pt-4 md:pt-6">



      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">



        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />



        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />



        <div className="relative grid gap-6 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
          <div>
            <span className="section-kicker">{'\u6587\u4ef6\u7ba1\u7406'}</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">{'\u8d44\u6599\u6574\u7406'}</span>
              <span className="chip">{'\u7f13\u5b58\u6e05\u7406'}</span>
              <span className="chip">{'\u7248\u672c\u7ba1\u7406'}</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">{'\u7edf\u4e00\u4f60\u7684\u6587\u4ef6\u8d44\u6599\u7ba1\u7406'}</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              {'\u652f\u6301\u641c\u7d22\u3001\u4e0a\u4f20\u3001\u5206\u7c7b\u4e0e\u7248\u672c\u8ffd\u8e2a\uff0c\u65b9\u4fbf\u5728\u65c5\u6e38\u4e1a\u52a1\u4e2d\u7edf\u4e00\u7ef4\u62a4\u8d44\u6e90\u6587\u4ef6\u3002'}
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u6587\u4ef6\u6570'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{totalFiles}</div>
              </div>
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5206\u7c7b\u6570'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{totalCategories}</div>
              </div>
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5f53\u524d\u5217\u8868'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{files.length}</div>
              </div>
            </div>
          </div>

          <div className="scenic-shell-soft edge-glow animate-fade-in p-5 sm:p-6">
            <div className="mb-4 flex items-center justify-between gap-3">
              <div>
                <div className="text-sm font-medium text-slate-500">{'\u5feb\u901f\u5165\u53e3'}</div>
                <div className="mt-1 text-xl font-semibold text-slate-900">{'\u6587\u4ef6\u641c\u7d22\u4e0e\u5206\u7c7b\u7ba1\u7406'}</div>
              </div>
              <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">{'\u6e05\u6670'}</span>
            </div>

            <input
              value={keyword}
              onChange={(event) => doSearch(event.target.value)}
              type="text"
              placeholder={'\u641c\u7d22\u6587\u4ef6\u540d\u3001\u5206\u7c7b\u6216\u8bf4\u660e'}
              className="search-input"
            />

            <div className="mt-4 grid gap-2 text-sm text-slate-500">
              <div className="travel-step-card">{'\u652f\u6301\u4e0a\u4f20\u3001\u68c0\u7d22\u548c\u7248\u672c\u8ffd\u8e2a'}</div>
              <div className="travel-step-card">{'\u5206\u7c7b\u7ba1\u7406\u8ba9\u8d44\u6e90\u66f4\u6613\u7ef4\u62a4'}</div>
            </div>

            <div className="mt-4 flex flex-wrap gap-3">
              <label className="btn-primary cursor-pointer">
                {'\u4e0a\u4f20\u6587\u4ef6'}
                <input type="file" className="hidden" onChange={uploadFile} />
              </label>

              <button type="button" onClick={() => setShowCategoryManager((v) => !v)} className="btn-secondary">
                {showCategoryManager ? '\u6536\u8d77\u5206\u7c7b' : '\u7ba1\u7406\u5206\u7c7b'}
              </button>
            </div>
          </div>
        </div>
      </section>



      {showCategoryManager ? (



        <section className="mt-8 scenic-shell-soft edge-glow animate-fade-in p-6">



          <div className="mb-4 flex items-center justify-between gap-3">



            <div>



              <span className="section-kicker">{'\u5206\u7c7b\u7ba1\u7406'}</span>



              <h2 className="mt-3 text-lg font-semibold text-slate-900">Categories</h2>



              <p className="mt-1 text-sm text-slate-500">维护文件分类，方便后续查找和沉淀。</p>



            </div>



            <span className="chip">{categories.length} 项</span>



          </div>



          <div className="flex flex-col gap-3 sm:flex-row">



            <input



              value={newCategoryName}



              onChange={(event) => setNewCategoryName(event.target.value)}



              type="text"



              placeholder="输入新的分类名称"



              className="search-input min-w-0 flex-1"



            />



            <button type="button" onClick={createCategory} className="btn-primary shrink-0">



              新增分类



            </button>



          </div>



          {categories.length ? (



            <div className="mt-4 flex flex-wrap gap-2">



              {categories.map((item) => (



                <span key={item.id} className="chip">



                  {item.tagName}



                  <button type="button" onClick={() => deleteCategory(item.id)} className="ml-2 text-xs text-red-500 hover:text-red-600">



                    ×



                  </button>



                </span>



              ))}



            </div>



          ) : (



            <div className="mt-4 py-6 text-center text-sm text-slate-400">暂时还没有分类</div>



          )}



        </section>



      ) : null}



      <div className="mb-6 mt-10 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">



        <div>



          <h2 className="section-heading text-[1.75rem]">文件列表</h2>



          <p className="section-subtitle mt-2">查看已上传文件、分类信息以及版本管理入口。</p>



        </div>



        <span className="chip">共 {files.length} 个</span>



      </div>



      {loading ? <LoadingSpinner /> : null}



      {!loading && !files.length ? (



        <div className="scenic-shell-soft edge-glow animate-fade-in p-10 text-center text-sm text-slate-500">



          暂未找到文件内容



        </div>



      ) : null}



      {!loading && files.length ? (



        <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">



          {files.map((item, index) => (



            <article key={item.id} className="scenic-shell-soft surface-card-hover animate-scale-in overflow-hidden" style={{ animationDelay: `${index * 60}ms` }}>



              <div className="p-5">



                <div className="flex items-start justify-between gap-3">



                  <div>



                    <h3 className="text-lg font-semibold text-slate-900">{item.fileName}</h3>



                    <p className="mt-1 text-xs text-slate-400">{item.category || '未分类'}</p>



                  </div>



                  <span className="rounded-full bg-sky-100 px-2.5 py-1 text-xs font-medium text-sky-700">{item.version || 'v1'}</span>



                </div>



                <p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-500">{item.description || '暂无文件说明'}</p>



                <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-400">



                  <span className="chip">大小 {item.size || '未知'}</span>



                  <span className="chip">上传于 {item.uploadTime || '未知时间'}</span>



                </div>



                <div className="mt-4 flex gap-2">



                  <button type="button" onClick={() => showVersions(item)} className="btn-secondary px-3 py-2 text-xs">版本记录</button>



                  <button



                    type="button"



                    onClick={() => deleteFile(item)}



                    className="rounded-2xl border border-red-200 px-3 py-2 text-xs font-medium text-red-600 transition hover:bg-red-50"



                  >



                    删除



                  </button>



                </div>



              </div>



            </article>



          ))}



        </section>



      ) : null}



      {versionModal.open ? (



        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4">



          <div className="scenic-shell-soft edge-glow max-h-[80vh] w-full max-w-3xl overflow-auto p-6">



            <div className="flex items-start justify-between gap-3">



              <div>



                <h2 className="text-lg font-semibold text-slate-900">版本记录 · {versionModal.fileName}</h2>



                <p className="mt-1 text-sm text-slate-500">查看历史版本，并与当前最新版本进行对比。</p>



              </div>



              <button



                type="button"



                onClick={() => setVersionModal({ open: false, loading: false, fileId: '', fileName: '', versions: [] })}



                className="rounded-full border border-slate-200 px-3 py-1 text-sm text-slate-500 transition hover:bg-slate-50"



              >



                关闭



              </button>



            </div>



            {versionModal.loading ? <LoadingSpinner /> : null}



            {!versionModal.loading && !versionModal.versions.length ? <div className="py-10 text-center text-sm text-slate-400">暂无历史版本</div> : null}



            {!versionModal.loading && versionModal.versions.length ? (



              <div className="mt-5 space-y-4">



                {versionModal.versions.map((version, index) => (



                  <div key={version.id || index} className="metric-card surface-card-hover">



                    <div className="flex items-center justify-between gap-3">



                      <div>



                        <div className="text-sm font-medium text-slate-900">{version.version || `版本 ${index + 1}`}</div>



                        <div className="mt-1 text-xs text-slate-400">{version.uploadTime || version.createTime || '未知时间'}</div>



                      </div>



                      <button type="button" onClick={() => compareWithLatest(version)} className="btn-secondary px-3 py-2 text-xs">



                        对比最新版本



                      </button>



                    </div>



                  </div>



                ))}



              </div>



            ) : null}



            {compareResult ? (



              <div className="mt-5 whitespace-pre-wrap rounded-2xl bg-slate-50 px-4 py-4 text-sm text-slate-600">



                {JSON.stringify(compareResult, null, 2)}



              </div>



            ) : null}



          </div>



        </div>



      ) : null}



    </div>



  )



}



