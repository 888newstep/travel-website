import apiClient from '../utils/api'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, DEFAULT_LIMIT } from '../constants'

export interface TravelNote {
  id: number
  title: string
  author: string
  image: string
  likes: number
  excerpt: string
  content?: string
  isLiked?: boolean
  isCollected?: boolean
  createTime?: string
}

type UnknownRecord = Record<string, unknown>

function asRecord(value: unknown): UnknownRecord {
  return value && typeof value === 'object' ? (value as UnknownRecord) : {}
}

function pickString(...values: unknown[]): string | undefined {
  for (const value of values) {
    if (typeof value === 'string' && value) {
      return value
    }
  }
  return undefined
}

function normalizeTravelNote(value: unknown): TravelNote {
  const envelope = asRecord(value)
  const source = envelope.travelNote ? asRecord(envelope.travelNote) : envelope
  const user = asRecord(source.user)
  const content = typeof source.content === 'string' ? source.content : ''

  return {
    id: Number(source.id) || 0,
    title: typeof source.title === 'string' ? source.title : '',
    author: pickString(source.author, user.nickname, user.username) || '',
    image: String(source.image || source.coverImage || ''),
    likes: Number(source.likes ?? source.likesCount) || 0,
    excerpt: String(source.excerpt || content),
    content,
    isLiked: Boolean(source.isLiked),
    isCollected: Boolean(source.isCollected),
    createTime: pickString(source.createTime, source.createdAt),
  }
}

function normalizeTravelNotes(value: unknown): TravelNote[] {
  return Array.isArray(value) ? value.map(normalizeTravelNote) : []
}

export const noteApi = {
  getNotes(page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE, filters?: Record<string, unknown>) {
    return apiClient
      .get<TravelNote[]>('/travel-notes/list', {
        params: { page, size, ...filters },
      })
      .then(normalizeTravelNotes)
  },

  getHotNotes(limit: number = DEFAULT_LIMIT) {
    return apiClient
      .get<TravelNote[]>('/travel-notes/hot', {
        params: { limit },
      })
      .then(normalizeTravelNotes)
  },

  getLatestNotes(limit: number = DEFAULT_LIMIT) {
    return apiClient
      .get<TravelNote[]>('/travel-notes/latest', {
        params: { limit },
      })
      .then(normalizeTravelNotes)
  },

  getNoteById(id: number) {
    return apiClient.get<unknown>(`/travel-notes/${id}`).then(normalizeTravelNote)
  },

  getUserTravelNotes(userId: number, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
    return apiClient
      .get<TravelNote[]>(`/travel-notes/user/${userId}`, {
        params: { page, size },
      })
      .then(normalizeTravelNotes)
  },

  searchNotes(keyword: string, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
    return apiClient
      .get<TravelNote[]>('/travel-notes/search', {
        params: { keyword, page, size },
      })
      .then(normalizeTravelNotes)
  },
}
