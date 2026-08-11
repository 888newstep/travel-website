import { LoadingDots } from '../components/common/LoadingDots'

import type { ReactNode } from 'react'

import { stringifyResult, useAIChatPage } from '../hooks/useAIChatPage'



function ResultCard({ value }: { value: unknown }) {

  return (

    <pre className="scenic-shell-soft whitespace-pre-wrap p-4 text-xs text-slate-600">

      {stringifyResult(value)}

    </pre>

  )

}



function SectionHint({ title, description }: { title: string; description: string }) {

  return (

    <div className="mb-4 flex flex-col gap-1">

      <h2 className="text-xl font-semibold text-slate-900">{title}</h2>

      <p className="text-sm text-slate-500">{description}</p>

    </div>

  )

}



function MessageBubble({ role, content }: { role: 'user' | 'assistant'; content: string }) {

  return (

    <div

      className={`rounded-2xl px-4 py-3 text-sm ${

        role === 'user'

          ? 'ml-auto max-w-[80%] bg-gradient-to-r from-sky-500 to-emerald-500 text-white'

          : 'max-w-[85%] border border-white/80 bg-white/85 text-slate-700 shadow-sm'

      }`}

    >

      {content}

    </div>

  )

}



function FeedbackPanel({ error, loading, children }: { error?: string | null; loading?: boolean; children?: ReactNode }) {

  return (

    <>

      {error ? <div className="mt-4 text-sm text-red-600">{error}</div> : null}

      {loading ? (

        <div className="mt-4">

          <LoadingDots />

        </div>

      ) : null}

      {children}

    </>

  )

}



export function AIChatPage() {

  const {

    tabs,

    activeTab,

    setActiveTab,

    input,

    setInput,

    loading,

    messages,

    recommendForm,

    setRecommendForm,

    recommendLoading,

    recommendError,

    recommendResults,

    itineraryForm,

    setItineraryForm,

    itineraryLoading,

    itineraryError,

    itineraryResult,

    imageForm,

    setImageForm,

    imageTypeOptions,

    imageLoading,

    imageResult,

    imageError,

    multimodalForm,

    setMultimodalForm,

    multimodalLoading,

    multimodalResult,

    budgetForm,

    setBudgetForm,

    budgetLoading,

    budgetResult,

    budgetError,

    assistantInput,

    setAssistantInput,

    assistantLoading,

    assistantMessages,

    planForm,

    setPlanForm,

    planLoading,

    planResult,

    planError,

    safetyCityId,

    setSafetyCityId,

    safetyLoading,

    safetyResult,

    safetyError,

    voiceText,

    setVoiceText,

    voiceLoading,

    voiceResult,

    voiceError,

    chatBoxRef,

    assistantBoxRef,

    totalMessages,

    activeLabel,

    sendMessage,

    getRecommendation,

    generateItinerary,

    analyzeImage,

    runMultimodalQuery,

    getBudgetAdvice,

    sendAssistantQuery,

    planRoute,

    fetchSafetyAdvice,

    processVoice,

  } = useAIChatPage()



  return (

    <div className="app-container animate-fade-in pb-16 pt-4 md:pt-6">

      <section className="scenic-shell edge-glow animate-slide-up overflow-hidden px-6 py-7 sm:px-8 sm:py-8">

        <div className="scenic-orb scenic-orb-sky -left-16 top-8 h-40 w-40" />

        <div className="scenic-orb scenic-orb-emerald right-0 top-0 h-48 w-48" />



        <div className="relative">

          <div className="grid gap-8 xl:grid-cols-[1.08fr_0.92fr] xl:items-center">

          <div>

            <span className="section-kicker">智能助手</span>

            <div className="mt-3 flex flex-wrap gap-2">

              <span className="chip">AI 旅行助手</span>

              <span className="chip">多模态服务</span>

              <span className="chip">智能出行建议</span>

            </div>

            <h1 className="text-3xl font-semibold tracking-tight text-slate-900 md:text-4xl">让 AI 成为你的随行旅行顾问</h1>

            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">

              这里整合了对话问答、景点推荐、路线规划、预算估算与语音处理等能力，界面已统一为更适合 Edge 浏览器的轻量旅游风格。

            </p>

            <div className="mt-6 grid gap-3 sm:grid-cols-3">

              <div className="metric-card rounded-2xl px-4 py-4">

                <div className="text-xs text-slate-500">功能模块</div>

                <div className="mt-2 text-2xl font-semibold text-slate-900">{tabs.length}</div>

              </div>

              <div className="metric-card rounded-2xl px-4 py-4">

                <div className="text-xs text-slate-500">当前模式</div>

                <div className="mt-2 text-2xl font-semibold text-slate-900">{activeLabel}</div>

              </div>

              <div className="metric-card rounded-2xl px-4 py-4">

                <div className="text-xs text-slate-500">会话消息</div>

                <div className="mt-2 text-2xl font-semibold text-slate-900">{totalMessages}</div>

              </div>

            </div>

          </div>



          <div className="scenic-shell-soft edge-glow animate-scale-in p-6">

            <div className="text-sm font-medium text-slate-500">推荐使用方式</div>

            <div className="mt-2 space-y-3 text-sm text-slate-600">

              <div className="travel-step-card">先用“智能推荐”快速筛一轮目的地和旅行偏好。</div>

              <div className="travel-step-card">再用“行程生成”或“路线规划”细化每日安排。</div>

              <div className="travel-step-card">出发前补一轮“预算估算”和“安全建议”会更稳妥。</div>

            </div>

          </div>

        </div>
        </div>
      </section>



      <section className="scenic-shell-soft edge-glow mb-6 mt-8 animate-fade-in p-4 sm:p-5">

        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">

          <div>

            <h2 className="section-heading text-[1.75rem]">功能导航</h2>

            <p className="section-subtitle mt-2">按旅行场景切换 AI 能力，减少重复操作与表单跳转。</p>

          </div>

          <span className="chip">当前：{activeLabel}</span>

        </div>

        <div className="scrollbar-hide flex gap-1 overflow-x-auto rounded-full border border-sky-200/70 bg-sky-50/90 p-2">

          {tabs.map((tab) => (

            <button

              key={tab.key}

              type="button"

              onClick={() => setActiveTab(tab.key)}

              className={`tab-pill ${activeTab === tab.key ? 'tab-pill-active' : 'tab-pill-idle'}`}

            >

              {tab.label}

            </button>

          ))}

        </div>

      </section>



      {activeTab === 'chat' ? (

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <SectionHint title="AI 对话" description="适合提问目的地攻略、交通方式、天气准备和景点选择。" />

          <div ref={chatBoxRef} className="h-96 space-y-3 overflow-y-auto rounded-2xl border border-slate-100 bg-white/75 p-4">

            {!messages.length ? (

              <div className="rounded-2xl border border-dashed border-sky-200 bg-white/80 px-4 py-4 text-sm text-slate-500">

                试试输入：三天两晚杭州轻松游怎么安排？

              </div>

            ) : null}

            {messages.map((message, index) => (

              <MessageBubble key={index} role={message.role} content={message.content} />

            ))}

            {loading ? (

              <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-500">

                <LoadingDots />

              </div>

            ) : null}

          </div>

          <div className="mt-4 flex gap-3">

            <input

              value={input}

              onChange={(event) => setInput(event.target.value)}

              onKeyDown={(event) => event.key === 'Enter' && sendMessage()}

              placeholder="输入你的旅行问题，例如：亲子游适合住在哪个区域？"

              className="search-input flex-1"

            />

            <button onClick={sendMessage} className="btn-primary">

              发送

            </button>

          </div>

        </section>

      ) : null}



      {activeTab === 'recommend' ? (

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <SectionHint title="智能推荐" description="根据城市、预算、时长与偏好返回更匹配的景点或旅行建议。" />

          <div className="grid gap-4 md:grid-cols-2">

            <input

              value={recommendForm.location}

              onChange={(event) => setRecommendForm((current) => ({ ...current, location: event.target.value }))}

              placeholder="出发地或目的地"

              className="search-input"

            />

            <input

              value={recommendForm.budget}

              onChange={(event) => setRecommendForm((current) => ({ ...current, budget: event.target.value }))}

              placeholder="预算，例如 3000"

              className="search-input"

            />

            <input

              value={recommendForm.duration}

              onChange={(event) => setRecommendForm((current) => ({ ...current, duration: event.target.value }))}

              placeholder="天数，例如 3"

              className="search-input"

            />

            <input

              value={recommendForm.preferences}

              onChange={(event) => setRecommendForm((current) => ({ ...current, preferences: event.target.value }))}

              placeholder="偏好，用逗号分隔：自然风光, 美食, 亲子"

              className="search-input"

            />

          </div>

          <button onClick={getRecommendation} className="btn-primary mt-4">

            获取推荐

          </button>

          <FeedbackPanel error={recommendError} loading={recommendLoading}>

            {recommendResults.length ? (

              <div className="mt-4 grid gap-4 md:grid-cols-2">

                {recommendResults.map((item, index) => (

                  <ResultCard key={index} value={item} />

                ))}

              </div>

            ) : null}

          </FeedbackPanel>

        </section>

      ) : null}



      {activeTab === 'itinerary' ? (

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <SectionHint title="行程生成" description="为指定目的地生成多日行程，适合出行前快速起草方案。" />

          <div className="grid gap-4 md:grid-cols-3">

            <input

              value={itineraryForm.destination}

              onChange={(event) => setItineraryForm((current) => ({ ...current, destination: event.target.value }))}

              placeholder="目的地，例如 苏州"

              className="search-input"

            />

            <input

              value={itineraryForm.days}

              onChange={(event) => setItineraryForm((current) => ({ ...current, days: event.target.value }))}

              placeholder="旅行天数"

              className="search-input"

            />

            <input

              value={itineraryForm.budget}

              onChange={(event) => setItineraryForm((current) => ({ ...current, budget: event.target.value }))}

              placeholder="预算，可选"

              className="search-input"

            />

          </div>

          <button onClick={generateItinerary} className="btn-primary mt-4">

            生成行程

          </button>

          <FeedbackPanel error={itineraryError} loading={itineraryLoading}>

            {itineraryResult ? (

              <div className="mt-4">

                <ResultCard value={itineraryResult} />

              </div>

            ) : null}

          </FeedbackPanel>

        </section>

      ) : null}



      {activeTab === 'image' ? (

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <SectionHint title="图像识别" description="识别景点、美食或图片中的文字内容，适合旅行中快速辅助判断。" />

          <div className="grid gap-4 md:grid-cols-[1fr_220px]">

            <input

              value={imageForm.url}

              onChange={(event) => setImageForm((current) => ({ ...current, url: event.target.value }))}

              placeholder="输入图片地址"

              className="search-input"

            />

            <select

              value={imageForm.type}

              onChange={(event) => setImageForm((current) => ({ ...current, type: event.target.value }))}

              className="search-input"

            >

              {imageTypeOptions.map((item) => (

                <option key={item.value} value={item.value}>

                  {item.label}

                </option>

              ))}

            </select>

          </div>

          <button onClick={analyzeImage} className="btn-primary mt-4">

            开始识别

          </button>

          <FeedbackPanel error={imageError} loading={imageLoading}>

            {imageResult ? (

              <div className="mt-4">

                <ResultCard value={imageResult} />

              </div>

            ) : null}

          </FeedbackPanel>

        </section>

      ) : null}



      {activeTab === 'multimodal' ? (

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <SectionHint title="多模态问答" description="同时结合文字与图片信息发起查询，适合问招牌、菜品或景点介绍。" />

          <div className="grid gap-4">

            <textarea

              value={multimodalForm.text}

              onChange={(event) => setMultimodalForm((current) => ({ ...current, text: event.target.value }))}

              rows={4}

              placeholder="描述你的问题，例如：这张图片里的建筑是什么风格？"

              className="search-input"

            />

            <input

              value={multimodalForm.image}

              onChange={(event) => setMultimodalForm((current) => ({ ...current, image: event.target.value }))}

              placeholder="图片地址，可选"

              className="search-input"

            />

          </div>

          <button onClick={runMultimodalQuery} className="btn-primary mt-4">

            提交查询

          </button>

          <FeedbackPanel loading={multimodalLoading}>

            {multimodalResult ? (

              <div className="mt-4 whitespace-pre-wrap rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">

                {multimodalResult}

              </div>

            ) : null}

          </FeedbackPanel>

        </section>

      ) : null}



      {activeTab === 'budget' ? (

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <SectionHint title="预算估算" description="按目的地、出行人数和风格估算费用，适合出发前做成本预案。" />

          <div className="grid gap-4 md:grid-cols-2">

            <input

              value={budgetForm.destination}

              onChange={(event) => setBudgetForm((current) => ({ ...current, destination: event.target.value }))}

              placeholder="目的地"

              className="search-input"

            />

            <input

              value={budgetForm.days}

              onChange={(event) => setBudgetForm((current) => ({ ...current, days: event.target.value }))}

              placeholder="天数"

              className="search-input"

            />

            <input

              value={budgetForm.budget}

              onChange={(event) => setBudgetForm((current) => ({ ...current, budget: event.target.value }))}

              placeholder="预算上限"

              className="search-input"

            />

            <input

              value={budgetForm.people}

              onChange={(event) => setBudgetForm((current) => ({ ...current, people: event.target.value }))}

              placeholder="人数"

              className="search-input"

            />

            <input

              value={budgetForm.style}

              onChange={(event) => setBudgetForm((current) => ({ ...current, style: event.target.value }))}

              placeholder="旅行风格，例如 轻奢 / 亲子 / 穷游"

              className="search-input md:col-span-2"

            />

          </div>

          <button onClick={getBudgetAdvice} className="btn-primary mt-4">

            估算预算

          </button>

          <FeedbackPanel error={budgetError} loading={budgetLoading}>

            {budgetResult ? (

              <div className="mt-4 whitespace-pre-wrap rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">

                {budgetResult}

              </div>

            ) : null}

          </FeedbackPanel>

        </section>

      ) : null}



      {activeTab === 'assistant' ? (

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <SectionHint title="旅行顾问" description="更适合连续追问和补充上下文，像和真人顾问一样逐步完善行程。" />

          <div ref={assistantBoxRef} className="h-96 space-y-3 overflow-y-auto rounded-2xl border border-slate-100 bg-white/75 p-4">

            {!assistantMessages.length ? (

              <div className="rounded-2xl border border-dashed border-sky-200 bg-white/80 px-4 py-4 text-sm text-slate-500">

                试试输入：我预算 4000，两个人周末去上海，想兼顾美食和拍照。

              </div>

            ) : null}

            {assistantMessages.map((message, index) => (

              <MessageBubble key={index} role={message.role} content={message.content} />

            ))}

            {assistantLoading ? (

              <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-500">

                <LoadingDots />

              </div>

            ) : null}

          </div>

          <div className="mt-4 flex gap-3">

            <input

              value={assistantInput}

              onChange={(event) => setAssistantInput(event.target.value)}

              onKeyDown={(event) => event.key === 'Enter' && sendAssistantQuery()}

              placeholder="继续补充你的需求，例如：希望住地铁旁，步行强度不要太高"

              className="search-input flex-1"

            />

            <button onClick={sendAssistantQuery} className="btn-primary">

              发送

            </button>

          </div>

        </section>

      ) : null}



      {activeTab === 'plan' ? (

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <SectionHint title="路线规划" description="通过偏好和限制条件生成结构化路线，适合复杂场景下快速成稿。" />

          <input

            value={planForm.preferences}

            onChange={(event) => setPlanForm((current) => ({ ...current, preferences: event.target.value }))}

            placeholder={'\u504f\u597d\uff0c\u4f8b\u5982\uff1a\u6175\u61d2\u8282\u594f\u3001\u4eb2\u5b50\u540c\u884c\u3001\u4e2d\u7b49\u9884\u7b97'}

            className="search-input"

          />

          <input

            value={planForm.constraints}

            onChange={(event) => setPlanForm((current) => ({ ...current, constraints: event.target.value }))}

            placeholder={'\u9650\u5236\uff0c\u4f8b\u5982\uff1a\u5e26\u5b69\u5b50\u3001\u4e09\u5929\u884c\u7a0b\u3001\u4ee5\u5730\u94c1\u4e3a\u4e3b'}

            className="search-input mt-4"

          />

          <button onClick={planRoute} className="btn-primary mt-4">

            生成路线

          </button>

          <FeedbackPanel error={planError} loading={planLoading}>

            {planResult ? (

              <div className="mt-4">

                <ResultCard value={planResult} />

              </div>

            ) : null}

          </FeedbackPanel>

        </section>

      ) : null}



      {activeTab === 'safety' ? (

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <SectionHint title="安全建议" description="按城市查询出行提醒，适合出发前核对天气、拥堵和特殊注意事项。" />

          <div className="flex gap-3">

            <input

              value={safetyCityId}

              onChange={(event) => setSafetyCityId(event.target.value)}

              placeholder={'\u8f93\u5165\u57ce\u5e02\u7f16\u53f7'}

              className="search-input flex-1"

            />

            <button onClick={fetchSafetyAdvice} className="btn-primary">

              查询

            </button>

          </div>

          <FeedbackPanel error={safetyError} loading={safetyLoading}>

            {safetyResult ? (

              <div className="mt-4">

                <ResultCard value={safetyResult} />

              </div>

            ) : null}

          </FeedbackPanel>

        </section>

      ) : null}



      {activeTab === 'voice' ? (

        <section className="scenic-shell-soft edge-glow animate-fade-in p-6">

          <SectionHint title="语音处理" description="把语音内容转为可理解文本后再交给 AI 处理，适合移动端快速记录灵感。" />

          <textarea

            value={voiceText}

            onChange={(event) => setVoiceText(event.target.value)}

            rows={4}

            placeholder="输入语音转写文本，例如：帮我整理成明天的行程重点"

            className="search-input"

          />

          <button onClick={processVoice} className="btn-primary mt-4">

            开始处理

          </button>

          <FeedbackPanel error={voiceError} loading={voiceLoading}>

            {voiceResult ? (

              <div className="mt-4 whitespace-pre-wrap rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">

                {voiceResult}

              </div>

            ) : null}

          </FeedbackPanel>

        </section>

      ) : null}

    </div>

  )

}

