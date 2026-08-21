import { useEffect, useMemo, useState } from 'react'

import { feedbackApi, type Feedback } from '../api/notification-feedback.api'

import { userApi } from '../api/user.api'

import { LoadingSpinner } from '../components/common/LoadingSpinner'

import { FEEDBACK_MAX_LENGTH } from '../constants'



const DEFAULT_FEEDBACK_TYPES = [

  { value: 'suggestion', label: '建议' },

  { value: 'bug', label: '故障反馈' },

  { value: 'complaint', label: '投诉' },

  { value: 'other', label: '其他' },

]



function formatTime(value?: string) {

  if (!value) return '未知'

  return value.replace('T', ' ').slice(0, 16)

}



function getStatusLabel(status?: string) {

  if (!status) return '待处理'

  return status

}



export function FeedbackPage() {

  const [feedbackTypes, setFeedbackTypes] = useState<{ value: string; label: string }[]>([])

  const [form, setForm] = useState({ type: '', content: '', contactInfo: '' })

  const [submitting, setSubmitting] = useState(false)

  const [submitted, setSubmitted] = useState(false)

  const [history, setHistory] = useState<Feedback[]>([])

  const [historyLoading, setHistoryLoading] = useState(true)

  const [currentUserId, setCurrentUserId] = useState<number | null>(null)



  const contentLength = useMemo(() => form.content.trim().length, [form.content])



  function typeLabel(type: string) {

    return feedbackTypes.find((item) => item.value === type)?.label || type

  }



  useEffect(() => {

    let active = true



    async function loadPage() {

      try {

        const types = await feedbackApi.getFeedbackTypes()

        if (active) setFeedbackTypes(Array.isArray(types) && types.length ? types : DEFAULT_FEEDBACK_TYPES)

      } catch {

        if (active) setFeedbackTypes(DEFAULT_FEEDBACK_TYPES)

      }



      try {

        const user = await userApi.getCurrentUser()

        if (!active) return



        setCurrentUserId(user.id)

        const records = await feedbackApi.getFeedbackList(user.id)

        if (active) setHistory(Array.isArray(records) ? records : [])

      } catch {

        if (active) setHistory([])

      } finally {

        if (active) setHistoryLoading(false)

      }

    }



    loadPage()

    return () => {

      active = false

    }

  }, [])



  async function submitFeedback(event: React.FormEvent<HTMLFormElement>) {

    event.preventDefault()

    if (!form.type || !form.content.trim() || submitting) return



    setSubmitting(true)

    setSubmitted(false)

    try {

      await feedbackApi.submitFeedback({

        type: form.type,

        content: form.content.trim(),

        contactInfo: form.contactInfo || undefined,

      })

      setSubmitted(true)

      setForm({ type: '', content: '', contactInfo: '' })

      if (currentUserId) {

        const records = await feedbackApi.getFeedbackList(currentUserId)

        setHistory(Array.isArray(records) ? records : [])

      }

    } catch {

      // ignore for now

    } finally {

      setSubmitting(false)

    }

  }



  return (

    <div className="app-container pb-16 pt-4 md:pt-6">

      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">

        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />

        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />



        <div className="relative grid gap-6 xl:grid-cols-[1.05fr_0.95fr] xl:items-center">
          <div>
            <span className="section-kicker">{'\u53cd\u9988\u4e2d\u5fc3'}</span>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="chip">{'\u4ea7\u54c1\u5efa\u8bae'}</span>
              <span className="chip">{'\u95ee\u9898\u8ddf\u8fdb'}</span>
              <span className="chip">{'\u670d\u52a1\u6539\u8fdb'}</span>
            </div>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">{'\u628a\u4f60\u7684\u65c5\u884c\u4f53\u9a8c\u53cd\u9988\u7ed9\u6211\u4eec'}</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
              {'\u65e0\u8bba\u662f\u4ea7\u54c1\u5efa\u8bae\u3001\u529f\u80fd\u5f02\u5e38\u8fd8\u662f\u670d\u52a1\u4f53\u9a8c\u95ee\u9898\uff0c\u90fd\u53ef\u4ee5\u5728\u8fd9\u91cc\u63d0\u4ea4\uff0c\u6211\u4eec\u4f1a\u6301\u7eed\u8ddf\u8fdb\u5e76\u4f18\u5316\u3002'}
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u53cd\u9988\u7c7b\u578b'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{feedbackTypes.length}</div>
              </div>
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5386\u53f2\u8bb0\u5f55'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{history.length}</div>
              </div>
              <div className="metric-card surface-card-hover">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{'\u5f53\u524d\u5b57\u6570'}</div>
                <div className="mt-3 text-2xl font-semibold text-slate-900">{contentLength}</div>
              </div>
            </div>
          </div>

          <div className="scenic-shell-soft edge-glow animate-fade-in p-6">
            <div className="flex items-center justify-between gap-3">
              <div>
                <div className="text-sm font-medium text-slate-500">{'\u5efa\u8bae\u6536\u96c6'}</div>
                <div className="mt-1 text-xl font-semibold text-slate-900">{'\u628a\u53cd\u9988\u76f4\u63a5\u9001\u5230\u56e2\u961f'}</div>
              </div>
              <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-medium text-sky-700">指南</span>
            </div>

            <div className="mt-4 grid gap-3 text-sm text-slate-600">
              <div className="metric-card surface-card-hover">{'\u4ea7\u54c1\u5efa\u8bae\u3001\u529f\u80fd\u5f02\u5e38\u90fd\u80fd\u63d0\u4ea4'}</div>
              <div className="metric-card surface-card-hover">{'\u652f\u6301\u8865\u5145\u8054\u7cfb\u65b9\u5f0f\uff0c\u4fbf\u4e8e\u8ddf\u8fdb'}</div>
              <div className="metric-card surface-card-hover">{'\u63d0\u4ea4\u540e\u53ef\u5728\u5386\u53f2\u4e2d\u67e5\u770b\u5904\u7406\u60c5\u51b5'}</div>
            </div>
          </div>
        </div>
      </section>



      <section className="mt-8 grid gap-6 xl:grid-cols-[0.92fr_1.08fr]">

        <form onSubmit={submitFeedback} className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <span className="section-kicker">{'\u63d0\u4ea4\u53cd\u9988'}</span>

          <h2 className="mt-3 text-lg font-semibold text-slate-900">Submit</h2>

          <p className="mt-1 text-sm text-slate-500">填写类型、内容和联系方式，方便后续跟进处理。</p>

          <div className="mt-5 space-y-4">

            <div>

              <label className="mb-1.5 block text-xs font-medium text-slate-500">反馈类型</label>

              <select

                value={form.type}

                onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))}

                className="search-input"

              >

                <option value="">请选择反馈类型</option>

                {feedbackTypes.map((item) => (

                  <option key={item.value} value={item.value}>{item.label}</option>

                ))}

              </select>

            </div>

            <div>

              <label className="mb-1.5 block text-xs font-medium text-slate-500">反馈内容</label>

              <textarea

                value={form.content}

                onChange={(event) => setForm((current) => ({ ...current, content: event.target.value.slice(0, FEEDBACK_MAX_LENGTH) }))}

                rows={6}

                placeholder="详细描述你遇到的问题或建议"

                className="search-input"

              />

              <div className="mt-2 text-right text-xs text-slate-400">{contentLength} / {FEEDBACK_MAX_LENGTH}</div>

            </div>

            <div>

              <label className="mb-1.5 block text-xs font-medium text-slate-500">联系方式</label>

              <input

                value={form.contactInfo}

                onChange={(event) => setForm((current) => ({ ...current, contactInfo: event.target.value }))}

                type="text"

                placeholder="邮箱、手机号或其他便于联系的方式"

                className="search-input"

              />

            </div>

            {submitted ? (

              <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">

                提交成功，我们会尽快查看并处理。

              </div>

            ) : null}

            <button

              type="submit"

              disabled={submitting || !form.type || !form.content.trim()}

              className="btn-primary w-full disabled:cursor-not-allowed disabled:opacity-60"

            >

              {submitting ? '提交中...' : 'Submit'}

            </button>

          </div>

        </form>



        <section className="scenic-shell-soft p-6">

          <span className="section-kicker">{'\u5386\u53f2\u8bb0\u5f55'}</span>

          <h2 className="mt-3 text-lg font-semibold text-slate-900">反馈历史</h2>

          <p className="mt-1 text-sm text-slate-500">查看已Submit的状态与回复内容。</p>

          {historyLoading ? <LoadingSpinner /> : null}

          {!historyLoading && !history.length ? <div className="py-10 text-center text-sm text-slate-400">还没有反馈记录</div> : null}

          {!historyLoading && history.length ? (

            <div className="mt-5 space-y-4">

              {history.map((item) => (

                <article key={item.id} className="metric-card surface-card-hover">

                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">

                    <div className="min-w-0 flex-1">

                      <div className="flex flex-wrap items-center gap-2">

                        <span className="rounded-full bg-white/90 px-2.5 py-1 text-xs font-medium text-slate-500">

                          {typeLabel(item.type)}

                        </span>

                        <span className="text-xs text-slate-400">{getStatusLabel(item.status)}</span>

                      </div>

                      <p className="mt-3 text-sm leading-6 text-slate-600">{item.content}</p>

                      {item.replyContent ? (

                        <div className="mt-3 rounded-2xl bg-white px-4 py-3 text-sm text-slate-500">

                          回复：{item.replyContent}

                        </div>

                      ) : null}

                    </div>

                    <div className="shrink-0 text-xs text-slate-400">{formatTime(item.createTime)}</div>

                  </div>

                </article>

              ))}

            </div>

          ) : null}

        </section>

      </section>

    </div>

  )

}

