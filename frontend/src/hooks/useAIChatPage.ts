import { useEffect, useMemo, useRef, useState } from 'react'
import { aiApi } from '../api/ai.api'

interface TabItem {
  key: string
  label: string
}

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const defaultTabs: TabItem[] = [
  { key: 'chat', label: 'AI 对话' },
  { key: 'recommend', label: '智能推荐' },
  { key: 'itinerary', label: '行程生成' },
  { key: 'image', label: '图像识别' },
  { key: 'assistant', label: '旅行顾问' },
  { key: 'plan', label: '路线规划' },
]

export function stringifyResult(value: unknown) {
  if (typeof value === 'string') {
    return value
  }

  return JSON.stringify(value, null, 2)
}

function getMessageText(value: any) {
  return value?.response || value?.data?.response || stringifyResult(value)
}

function getAIErrorMessage(error: unknown): string {
  const message = error instanceof Error ? error.message : ''

  if (message.includes('timeout')) {
    return 'AI 服务响应超时，请稍后重试。'
  }

  if (message.includes('401')) {
    return '当前 AI 接口鉴权失败，请重新登录或检查后端权限配置。'
  }

  return `请求失败${message ? `：${message}` : '，请稍后再试。'}`
}

function parseKeyValueText(value: string) {
  const result: Record<string, string | boolean> = {}

  value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .forEach((item) => {
      const [key, raw] = item.split(':')
      if (!key) {
        return
      }
      result[key.trim()] = raw ? raw.trim() : true
    })

  return result
}

export function useAIChatPage() {
  const tabs = defaultTabs
  const [activeTab, setActiveTab] = useState('chat')

  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])

  const [recommendForm, setRecommendForm] = useState({
    location: '',
    budget: '',
    duration: '',
    preferences: '',
  })
  const [recommendLoading, setRecommendLoading] = useState(false)
  const [recommendError, setRecommendError] = useState('')
  const [recommendResults, setRecommendResults] = useState<any[]>([])

  const [itineraryForm, setItineraryForm] = useState({ destination: '', days: '', budget: '' })
  const [itineraryLoading, setItineraryLoading] = useState(false)
  const [itineraryError, setItineraryError] = useState('')
  const [itineraryResult, setItineraryResult] = useState<any>(null)

  const [imageForm, setImageForm] = useState({ url: '', type: 'scene' })
  const [imageTypeOptions, setImageTypeOptions] = useState<{ value: string; label: string }[]>([
    { value: 'scene', label: '景点识别' },
    { value: 'dish', label: '美食识别' },
    { value: 'ocr', label: '文字提取' },
  ])
  const [imageLoading, setImageLoading] = useState(false)
  const [imageResult, setImageResult] = useState<any>(null)
  const [imageError, setImageError] = useState('')

  const [assistantInput, setAssistantInput] = useState('')
  const [assistantLoading, setAssistantLoading] = useState(false)
  const [assistantMessages, setAssistantMessages] = useState<ChatMessage[]>([])

  const [planForm, setPlanForm] = useState({ preferences: '', constraints: '' })
  const [planLoading, setPlanLoading] = useState(false)
  const [planResult, setPlanResult] = useState<any>(null)
  const [planError, setPlanError] = useState('')

  const chatBoxRef = useRef<HTMLDivElement | null>(null)
  const assistantBoxRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    aiApi
      .getImageAnalysisTypes()
      .then((response) => {
        if (Array.isArray(response) && response.length) {
          setImageTypeOptions(response)
        }
      })
      .catch(() => {})
  }, [])

  useEffect(() => {
    if (chatBoxRef.current) {
      chatBoxRef.current.scrollTop = chatBoxRef.current.scrollHeight
    }
  }, [messages, loading])

  useEffect(() => {
    if (assistantBoxRef.current) {
      assistantBoxRef.current.scrollTop = assistantBoxRef.current.scrollHeight
    }
  }, [assistantMessages, assistantLoading])

  const totalMessages = useMemo(
    () => messages.length + assistantMessages.length,
    [messages.length, assistantMessages.length],
  )
  const activeLabel = useMemo(
    () => tabs.find((tab) => tab.key === activeTab)?.label || 'AI 对话',
    [tabs, activeTab],
  )

  async function sendMessage() {
    const text = input.trim()
    if (!text || loading) {
      return
    }

    setMessages((current) => [...current, { role: 'user', content: text }])
    setInput('')
    setLoading(true)

    try {
      const response = await aiApi.chat({ message: text })
      setMessages((current) => [...current, { role: 'assistant', content: getMessageText(response) }])
    } catch (error) {
      setMessages((current) => [...current, { role: 'assistant', content: getAIErrorMessage(error) }])
    } finally {
      setLoading(false)
    }
  }

  async function getRecommendation() {
    setRecommendError('')
    setRecommendResults([])
    setRecommendLoading(true)

    const payload: Record<string, any> = {}
    if (recommendForm.location) payload.location = recommendForm.location
    if (recommendForm.budget) payload.budget = Number(recommendForm.budget)
    if (recommendForm.duration) payload.duration = Number(recommendForm.duration)
    if (recommendForm.preferences) {
      payload.preferences = recommendForm.preferences
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean)
    }

    try {
      const response = await aiApi.getTravelRecommendation(payload)
      const result = response?.data || response

      if (Array.isArray(result)) {
        setRecommendResults(result)
      } else if (Array.isArray(result?.recommendations)) {
        setRecommendResults(result.recommendations)
      } else {
        setRecommendResults([result])
      }
    } catch (error) {
      setRecommendError(getAIErrorMessage(error))
    } finally {
      setRecommendLoading(false)
    }
  }

  async function generateItinerary() {
    setItineraryError('')
    setItineraryResult(null)
    setItineraryLoading(true)

    try {
      const response = await aiApi.generateItinerary({
        destination: itineraryForm.destination || '热门城市',
        days: Number(itineraryForm.days) || 1,
        budget: itineraryForm.budget ? Number(itineraryForm.budget) : undefined,
      })
      setItineraryResult(response?.data || response)
    } catch (error) {
      setItineraryError(getAIErrorMessage(error))
    } finally {
      setItineraryLoading(false)
    }
  }

  async function analyzeImage() {
    if (!imageForm.url.trim()) {
      return
    }

    setImageError('')
    setImageResult(null)
    setImageLoading(true)

    try {
      const response = await aiApi.analyzeImage({
        imageUrl: imageForm.url,
        analysisType: imageForm.type,
      })
      setImageResult(response?.data || response)
    } catch (error) {
      setImageError(getAIErrorMessage(error))
    } finally {
      setImageLoading(false)
    }
  }

  async function sendAssistantQuery() {
    const query = assistantInput.trim()
    if (!query || assistantLoading) {
      return
    }

    setAssistantMessages((current) => [...current, { role: 'user', content: query }])
    setAssistantInput('')
    setAssistantLoading(true)

    try {
      const response = await aiApi.smartAssistant(query)
      setAssistantMessages((current) => [...current, { role: 'assistant', content: getMessageText(response) }])
    } catch (error) {
      setAssistantMessages((current) => [...current, { role: 'assistant', content: getAIErrorMessage(error) }])
    } finally {
      setAssistantLoading(false)
    }
  }

  async function planRoute() {
    setPlanError('')
    setPlanResult(null)
    setPlanLoading(true)

    try {
      const response = await aiApi.planSmartRoute({
        preferences: parseKeyValueText(planForm.preferences),
        constraints: parseKeyValueText(planForm.constraints),
      })
      setPlanResult(response?.data || response)
    } catch (error) {
      setPlanError(getAIErrorMessage(error))
    } finally {
      setPlanLoading(false)
    }
  }

  return {
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
    assistantInput,
    setAssistantInput,
    assistantLoading,
    assistantMessages,
    planForm,
    setPlanForm,
    planLoading,
    planResult,
    planError,
    chatBoxRef,
    assistantBoxRef,
    totalMessages,
    activeLabel,
    sendMessage,
    getRecommendation,
    generateItinerary,
    analyzeImage,
    sendAssistantQuery,
    planRoute,
  }
}
