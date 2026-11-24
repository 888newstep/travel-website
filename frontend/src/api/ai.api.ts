import apiClient from '../utils/api'

const AI_BASE = '/ai'
const AI_ADVANCED_BASE = `${AI_BASE}/advanced`

export interface AIChatRequest {
  message: string
  context?: Record<string, unknown>
}

export interface AIChatResponse {
  response: string
  suggestions?: string[]
}

export interface AIImageAnalysis {
  imageUrl: string
  analysisType?: string
}

/** AI 接口返回结构不固定：可能直接返回结果，也可能包一层 data / recommendations */
export interface AIResultEnvelope {
  data?: unknown
  recommendations?: unknown[]
  response?: string
  message?: string
  [key: string]: unknown
}

export const aiApi = {
  chat(data: AIChatRequest) {
    return apiClient.post<AIChatResponse>(`${AI_BASE}/chat`, data)
  },

  getTravelRecommendation(data: {
    userId?: number
    location?: string
    preferences?: Record<string, unknown>
    budget?: number
    duration?: number
  }) {
    return apiClient.post<AIResultEnvelope>(`${AI_BASE}/recommend`, data)
  },

  analyzeImage(data: AIImageAnalysis) {
    return apiClient.post<AIResultEnvelope>(`${AI_BASE}/image-analysis`, data)
  },

  getImageAnalysisTypes() {
    return apiClient.get<{ value: string; label: string }[]>(`${AI_BASE}/image-analysis/types`)
  },

  generateItinerary(data: {
    destination: string
    days: number
    preferences?: Record<string, unknown>
    budget?: number
  }) {
    return apiClient.post<AIResultEnvelope>(`${AI_BASE}/itinerary/generate`, data)
  },

  smartAssistant(query: string, context?: Record<string, unknown>) {
    return apiClient.post<AIChatResponse>(`${AI_BASE}/assistant/chat`, {
      query,
      context,
    })
  },

  planSmartRoute(data: Record<string, unknown>) {
    return apiClient.post<AIResultEnvelope>(`${AI_ADVANCED_BASE}/plan`, data)
  },
}
